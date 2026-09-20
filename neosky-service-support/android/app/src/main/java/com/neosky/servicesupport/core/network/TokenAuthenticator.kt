package com.neosky.servicesupport.core.network

import com.neosky.servicesupport.BuildConfig
import com.neosky.servicesupport.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements silent refresh-on-401: when any request comes back 401, this makes a single
 * synchronous call to `/auth/refresh` with the stored refresh token, stores the new access
 * token, and retries the original request once. If the refresh itself fails (expired/rotated
 * refresh token), the session is cleared and the request is allowed to fail, so the UI layer's
 * observation of [TokenManager.currentUser] naturally routes the user back to the login screen.
 *
 * A dedicated, interceptor-free [OkHttpClient] is used for the refresh call itself to avoid
 * recursively triggering this same authenticator.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops.
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        val newAccessToken = synchronized(this) {
            // Another thread may have already refreshed while we were waiting on the lock.
            val currentAccessToken = tokenManager.getAccessToken()
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentAccessToken != null && currentAccessToken != failedToken) {
                currentAccessToken
            } else {
                runBlocking { doRefresh(refreshToken) }
            }
        } ?: run {
            runBlocking { tokenManager.clearSession() }
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private suspend fun doRefresh(refreshToken: String): String? {
        return try {
            val body = """{"refresh_token":"$refreshToken"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.BASE_URL.trimEnd('/') + "/auth/refresh")
                .post(body)
                .build()
            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyString = resp.body?.string() ?: return null
                val element = json.parseToJsonElement(bodyString).jsonObject
                val accessToken = element["access_token"]?.jsonPrimitive?.content ?: return null
                tokenManager.updateAccessToken(accessToken)
                accessToken
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

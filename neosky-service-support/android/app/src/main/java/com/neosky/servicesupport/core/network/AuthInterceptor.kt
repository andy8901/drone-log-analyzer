package com.neosky.servicesupport.core.network

import com.neosky.servicesupport.core.datastore.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches `Authorization: Bearer <access_token>` to every request except the public auth endpoints. */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (NO_AUTH_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(original)
        }
        val token = tokenManager.getAccessToken() ?: return chain.proceed(original)
        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }

    companion object {
        private val NO_AUTH_PATHS = listOf(
            "/auth/register",
            "/auth/verify-otp",
            "/auth/login",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
        )
    }
}

package com.neosky.servicesupport.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.neosky.servicesupport.core.di.IoDispatcher
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure, at-rest storage for the access/refresh tokens and the logged-in user's identity,
 * backed by [EncryptedSharedPreferences] (AES256-GCM keys managed by the Android Keystore via
 * [MasterKey]). This is the single place in the app that ever reads or writes a raw token.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _currentUser = MutableStateFlow(readUserFromPrefs())
    val currentUser = _currentUser.asStateFlow()

    private val _rememberMe = MutableStateFlow(prefs.getBoolean(KEY_REMEMBER_ME, true))
    val rememberMe = _rememberMe.asStateFlow()

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        user: User,
        rememberMe: Boolean,
    ) = withContext(ioDispatcher) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHONE, user.phone)
            .putString(KEY_USER_FULL_NAME, user.fullName)
            .putString(KEY_USER_ROLE, user.role.wireValue)
            .putBoolean(KEY_REMEMBER_ME, rememberMe)
            .apply()
        _currentUser.value = user
        _rememberMe.value = rememberMe
    }

    suspend fun updateAccessToken(accessToken: String) = withContext(ioDispatcher) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getLoggedInUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null

    suspend fun clearSession() = withContext(ioDispatcher) {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun readUserFromPrefs(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val fullName = prefs.getString(KEY_USER_FULL_NAME, null) ?: return null
        return User(
            id = id,
            fullName = fullName,
            email = email,
            phone = prefs.getString(KEY_USER_PHONE, null),
            role = UserRole.fromWire(prefs.getString(KEY_USER_ROLE, null)),
        )
    }

    companion object {
        private const val PREFS_FILE_NAME = "neosky_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
}

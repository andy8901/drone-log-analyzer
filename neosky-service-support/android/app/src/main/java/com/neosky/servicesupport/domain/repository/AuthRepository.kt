package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits the currently logged-in user (from secure local storage), or null when logged out. */
    val currentUser: Flow<User?>

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        companyName: String?,
    ): NetworkResult<User>

    suspend fun verifyOtp(email: String, otp: String): NetworkResult<Boolean>

    suspend fun login(identifier: String, password: String, rememberMe: Boolean): NetworkResult<AuthSession>

    suspend fun forgotPassword(identifier: String): NetworkResult<String>

    suspend fun resetPassword(identifier: String, otp: String, newPassword: String): NetworkResult<String>

    suspend fun logout(): NetworkResult<Unit>

    /** Attempts silent auto-login from a previously stored session. */
    suspend fun tryAutoLogin(): Boolean
}

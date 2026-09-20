package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.datastore.TokenManager
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallUnit
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.data.remote.dto.ForgotPasswordRequestDto
import com.neosky.servicesupport.data.remote.dto.LoginRequestDto
import com.neosky.servicesupport.data.remote.dto.RegisterRequestDto
import com.neosky.servicesupport.data.remote.dto.ResetPasswordRequestDto
import com.neosky.servicesupport.data.remote.dto.VerifyOtpRequestDto
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
) : AuthRepository {

    override val currentUser: Flow<User?> = tokenManager.currentUser

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        companyName: String?,
    ): NetworkResult<User> {
        val result = safeApiCall {
            apiService.register(RegisterRequestDto(fullName, email, phone, password, companyName))
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.user.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): NetworkResult<Boolean> {
        val result = safeApiCall { apiService.verifyOtp(VerifyOtpRequestDto(email, otp)) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.verified)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun login(identifier: String, password: String, rememberMe: Boolean): NetworkResult<AuthSession> {
        val result = safeApiCall { apiService.login(LoginRequestDto(identifier, password)) }
        return when (result) {
            is NetworkResult.Success -> {
                val dto = result.data
                val user = dto.user.toDomain()
                tokenManager.saveSession(dto.accessToken, dto.refreshToken, user, rememberMe)
                NetworkResult.Success(AuthSession(dto.accessToken, dto.refreshToken, dto.expiresIn, user))
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun forgotPassword(identifier: String): NetworkResult<String> {
        val result = safeApiCall { apiService.forgotPassword(ForgotPasswordRequestDto(identifier)) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.message)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun resetPassword(identifier: String, otp: String, newPassword: String): NetworkResult<String> {
        val result = safeApiCall { apiService.resetPassword(ResetPasswordRequestDto(identifier, otp, newPassword)) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.message)
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val result = safeApiCallUnit { apiService.logout() }
        // Always clear the local session, even if the network call failed (e.g. offline logout):
        // there is no safe state in which the app should keep showing a token it told the user
        // it logged out of.
        tokenManager.clearSession()
        return result
    }

    override suspend fun tryAutoLogin(): Boolean = tokenManager.isLoggedIn()
}

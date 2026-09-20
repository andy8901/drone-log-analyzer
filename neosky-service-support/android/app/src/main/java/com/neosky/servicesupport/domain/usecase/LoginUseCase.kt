package com.neosky.servicesupport.domain.usecase

import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.repository.AuthRepository
import javax.inject.Inject

/** Basic client-side field validation ahead of the network call, plus trimming. */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(identifier: String, password: String, rememberMe: Boolean): NetworkResult<AuthSession> {
        val trimmedIdentifier = identifier.trim()
        if (trimmedIdentifier.isEmpty()) {
            return NetworkResult.Error(
                ApiException(422, "VALIDATION_ERROR", "Email or phone is required", mapOf("identifier" to "required")),
            )
        }
        if (password.isEmpty()) {
            return NetworkResult.Error(
                ApiException(422, "VALIDATION_ERROR", "Password is required", mapOf("password" to "required")),
            )
        }
        return authRepository.login(trimmedIdentifier, password, rememberMe)
    }
}

package com.neosky.servicesupport.presentation.auth

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-written test double for [AuthRepository] — avoids pulling MockK's Android instrumentation
 * variant into this simple, fully deterministic Compose UI test.
 */
class FakeAuthRepository(
    private val loginResult: NetworkResult<AuthSession>,
) : AuthRepository {

    override val currentUser = MutableStateFlow<User?>(null)

    override suspend fun register(fullName: String, email: String, phone: String, password: String, companyName: String?) =
        throw NotImplementedError("Not used by LoginScreenTest")

    override suspend fun verifyOtp(email: String, otp: String) = throw NotImplementedError("Not used by LoginScreenTest")

    override suspend fun login(identifier: String, password: String, rememberMe: Boolean): NetworkResult<AuthSession> = loginResult

    override suspend fun forgotPassword(identifier: String) = throw NotImplementedError("Not used by LoginScreenTest")

    override suspend fun resetPassword(identifier: String, otp: String, newPassword: String) =
        throw NotImplementedError("Not used by LoginScreenTest")

    override suspend fun logout() = throw NotImplementedError("Not used by LoginScreenTest")

    override suspend fun tryAutoLogin(): Boolean = false
}

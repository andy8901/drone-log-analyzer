package com.neosky.servicesupport.presentation.auth

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.neosky.servicesupport.MainDispatcherRule
import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.model.UserRole
import com.neosky.servicesupport.domain.usecase.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase: LoginUseCase = mockk()

    private fun viewModel() = LoginViewModel(loginUseCase)

    private val fakeSession = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 900,
        user = User(id = "u1", fullName = "Aniket Rao", email = "aniket@throttle.aero", phone = null, role = UserRole.CUSTOMER),
    )

    @Test
    fun `login success emits loginSuccess and clears loading`() = runTest {
        coEvery { loginUseCase(any(), any(), any()) } returns NetworkResult.Success(fakeSession)
        val viewModel = viewModel()

        viewModel.onIdentifierChanged("aniket@throttle.aero")
        viewModel.onPasswordChanged("NeoSky@123")

        viewModel.loginSuccess.test {
            viewModel.login()
            awaitItem()
        }

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.generalError).isNull()
    }

    @Test
    fun `login failure surfaces field errors and stops loading`() = runTest {
        val apiException = ApiException(
            httpCode = 401,
            code = "INVALID_CREDENTIALS",
            message = "Invalid email or password",
        )
        coEvery { loginUseCase(any(), any(), any()) } returns NetworkResult.Error(apiException)
        val viewModel = viewModel()

        viewModel.onIdentifierChanged("aniket@throttle.aero")
        viewModel.onPasswordChanged("wrong-password")
        viewModel.login()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.generalError).isEqualTo("Invalid email or password")
    }

    @Test
    fun `login failure with field errors maps them to identifier and password fields`() = runTest {
        val apiException = ApiException(
            httpCode = 422,
            code = "VALIDATION_ERROR",
            message = "Validation failed",
            fieldErrors = mapOf("identifier" to "required", "password" to "required"),
        )
        coEvery { loginUseCase(any(), any(), any()) } returns NetworkResult.Error(apiException)
        val viewModel = viewModel()

        viewModel.login()

        assertThat(viewModel.uiState.value.identifierError).isEqualTo("required")
        assertThat(viewModel.uiState.value.passwordError).isEqualTo("required")
        assertThat(viewModel.uiState.value.generalError).isNull()
    }

    @Test
    fun `changing identifier clears previous errors`() = runTest {
        val apiException = ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password")
        coEvery { loginUseCase(any(), any(), any()) } returns NetworkResult.Error(apiException)
        val viewModel = viewModel()
        viewModel.login()
        assertThat(viewModel.uiState.value.generalError).isNotNull()

        viewModel.onIdentifierChanged("new@throttle.aero")

        assertThat(viewModel.uiState.value.generalError).isNull()
    }
}

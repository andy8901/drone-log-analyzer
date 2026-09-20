package com.neosky.servicesupport.presentation.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.AuthSession
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.model.UserRole
import com.neosky.servicesupport.domain.usecase.LoginUseCase
import com.neosky.servicesupport.presentation.theme.NeoSkyServiceSupportTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fakeSession = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 900,
        user = User(id = "u1", fullName = "Aniket Rao", email = "aniket@throttle.aero", phone = null, role = UserRole.CUSTOMER),
    )

    @Test
    fun emptyFields_showsValidationErrors_onSubmit() {
        val viewModel = LoginViewModel(LoginUseCase(FakeAuthRepository(NetworkResult.Success(fakeSession))))
        composeTestRule.setContent {
            NeoSkyServiceSupportTheme {
                LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}, onNavigateToForgotPassword = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Sign in").performClick()

        composeTestRule.onNodeWithText("Email or phone is required").assertDoesNotExist()
        // The field-level validation message rendered as the identifier field's supporting text.
        composeTestRule.onNodeWithText("required").assertExists()
    }

    @Test
    fun validCredentials_triggersLoginSuccessCallback() {
        var loginSucceeded = false
        val viewModel = LoginViewModel(LoginUseCase(FakeAuthRepository(NetworkResult.Success(fakeSession))))
        composeTestRule.setContent {
            NeoSkyServiceSupportTheme {
                LoginScreen(
                    onLoginSuccess = { loginSucceeded = true },
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                    viewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText("Email or phone").performTextInput("aniket@throttle.aero")
        composeTestRule.onNodeWithText("Password").performTextInput("NeoSky@123")
        composeTestRule.onNodeWithText("Sign in").performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { loginSucceeded }
    }
}

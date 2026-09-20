package com.neosky.servicesupport.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegisterStep { FORM, OTP }

data class RegisterUiState(
    val step: RegisterStep = RegisterStep.FORM,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val companyName: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _registrationComplete = MutableSharedFlow<Unit>()
    val registrationComplete: SharedFlow<Unit> = _registrationComplete.asSharedFlow()

    fun onFieldChanged(fullName: String? = null, email: String? = null, phone: String? = null, password: String? = null, confirmPassword: String? = null, companyName: String? = null) {
        _uiState.value = _uiState.value.copy(
            fullName = fullName ?: _uiState.value.fullName,
            email = email ?: _uiState.value.email,
            phone = phone ?: _uiState.value.phone,
            password = password ?: _uiState.value.password,
            confirmPassword = confirmPassword ?: _uiState.value.confirmPassword,
            companyName = companyName ?: _uiState.value.companyName,
            fieldErrors = emptyMap(),
            generalError = null,
        )
    }

    fun onOtpChanged(value: String) {
        _uiState.value = _uiState.value.copy(otp = value, generalError = null)
    }

    fun submitRegistration() {
        val state = _uiState.value
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(fieldErrors = mapOf("confirmPassword" to "Passwords do not match"))
            return
        }
        if (state.fullName.isBlank() || state.email.isBlank() || state.phone.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(generalError = "Please fill in every required field")
            return
        }

        _uiState.value = state.copy(isLoading = true, generalError = null)
        viewModelScope.launch {
            val result = authRepository.register(
                fullName = state.fullName.trim(),
                email = state.email.trim(),
                phone = state.phone.trim(),
                password = state.password,
                companyName = state.companyName.ifBlank { null },
            )
            _uiState.value = when (result) {
                is NetworkResult.Success -> _uiState.value.copy(
                    isLoading = false,
                    step = RegisterStep.OTP,
                    infoMessage = "We've sent a verification code to ${state.email}",
                )
                is NetworkResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    fieldErrors = result.apiException.fieldErrors,
                    generalError = if (result.apiException.fieldErrors.isEmpty()) result.apiException.message else null,
                )
                NetworkResult.Loading -> _uiState.value
            }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.otp.isBlank()) {
            _uiState.value = state.copy(generalError = "Enter the code we sent you")
            return
        }
        _uiState.value = state.copy(isLoading = true, generalError = null)
        viewModelScope.launch {
            when (val result = authRepository.verifyOtp(state.email, state.otp)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _registrationComplete.emit(Unit)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, generalError = result.apiException.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}

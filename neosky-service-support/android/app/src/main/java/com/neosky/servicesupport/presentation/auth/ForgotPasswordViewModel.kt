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

enum class ForgotPasswordStep { REQUEST, RESET }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.REQUEST,
    val identifier: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _done = MutableSharedFlow<Unit>()
    val done: SharedFlow<Unit> = _done.asSharedFlow()

    fun onIdentifierChanged(value: String) {
        _uiState.value = _uiState.value.copy(identifier = value, error = null)
    }

    fun onOtpChanged(value: String) {
        _uiState.value = _uiState.value.copy(otp = value, error = null)
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, error = null)
    }

    fun requestOtp() {
        val state = _uiState.value
        if (state.identifier.isBlank()) {
            _uiState.value = state.copy(error = "Enter your email or phone")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepository.forgotPassword(state.identifier.trim())) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = ForgotPasswordStep.RESET,
                    message = result.data,
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.otp.isBlank() || state.newPassword.isBlank()) {
            _uiState.value = state.copy(error = "Enter the code and your new password")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepository.resetPassword(state.identifier.trim(), state.otp, state.newPassword)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _done.emit(Unit)
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

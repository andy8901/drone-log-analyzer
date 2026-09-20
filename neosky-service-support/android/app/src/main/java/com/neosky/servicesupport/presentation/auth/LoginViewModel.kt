package com.neosky.servicesupport.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val identifierError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess: SharedFlow<Unit> = _loginSuccess.asSharedFlow()

    fun onIdentifierChanged(value: String) {
        _uiState.value = _uiState.value.copy(identifier = value, identifierError = null, generalError = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, generalError = null)
    }

    fun onRememberMeChanged(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.value = state.copy(isLoading = true, generalError = null, identifierError = null, passwordError = null)

        viewModelScope.launch {
            when (val result = loginUseCase(state.identifier, state.password, state.rememberMe)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _loginSuccess.emit(Unit)
                }
                is NetworkResult.Error -> {
                    val ex = result.apiException
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        identifierError = ex.fieldErrors["identifier"],
                        passwordError = ex.fieldErrors["password"],
                        generalError = if (ex.fieldErrors.isEmpty()) ex.message else null,
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}

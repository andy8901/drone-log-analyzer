package com.neosky.servicesupport.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Customer
import com.neosky.servicesupport.domain.repository.AuthRepository
import com.neosky.servicesupport.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val customer: Customer? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val editFullName: String = "",
    val editPhone: String = "",
    val editCompanyName: String = "",
    val editBillingAddress: String = "",
    val editGstin: String = "",
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableSharedFlow<Unit>()
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = customerRepository.getProfile()) {
                is NetworkResult.Success -> _uiState.value = ProfileUiState(isLoading = false, customer = result.data, editFullName = result.data.fullName, editPhone = result.data.phone.orEmpty(), editCompanyName = result.data.companyName.orEmpty(), editBillingAddress = result.data.billingAddress.orEmpty(), editGstin = result.data.gstin.orEmpty())
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true)
    }

    fun cancelEditing() {
        val customer = _uiState.value.customer
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editFullName = customer?.fullName.orEmpty(),
            editPhone = customer?.phone.orEmpty(),
            editCompanyName = customer?.companyName.orEmpty(),
            editBillingAddress = customer?.billingAddress.orEmpty(),
            editGstin = customer?.gstin.orEmpty(),
        )
    }

    fun onFullNameChanged(value: String) = update { it.copy(editFullName = value) }
    fun onPhoneChanged(value: String) = update { it.copy(editPhone = value) }
    fun onCompanyNameChanged(value: String) = update { it.copy(editCompanyName = value) }
    fun onBillingAddressChanged(value: String) = update { it.copy(editBillingAddress = value) }
    fun onGstinChanged(value: String) = update { it.copy(editGstin = value) }

    private inline fun update(block: (ProfileUiState) -> ProfileUiState) {
        _uiState.value = block(_uiState.value)
    }

    fun saveProfile() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            when (val result = customerRepository.updateProfile(
                fullName = state.editFullName,
                phone = state.editPhone,
                companyName = state.editCompanyName,
                billingAddress = state.editBillingAddress,
                gstin = state.editGstin,
            )) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isSaving = false, isEditing = false, customer = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.emit(Unit)
        }
    }
}

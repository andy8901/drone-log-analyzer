package com.neosky.servicesupport.presentation.warranty

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Warranty
import com.neosky.servicesupport.domain.repository.WarrantyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WarrantyUiState(
    val isLoading: Boolean = true,
    val warranty: Warranty? = null,
    val error: String? = null,
)

@HiltViewModel
class WarrantyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val warrantyRepository: WarrantyRepository,
) : ViewModel() {

    private val droneId: String = checkNotNull(savedStateHandle["droneId"])

    private val _uiState = MutableStateFlow(WarrantyUiState())
    val uiState: StateFlow<WarrantyUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = warrantyRepository.getWarranty(droneId)) {
                is NetworkResult.Success -> _uiState.value = WarrantyUiState(isLoading = false, warranty = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

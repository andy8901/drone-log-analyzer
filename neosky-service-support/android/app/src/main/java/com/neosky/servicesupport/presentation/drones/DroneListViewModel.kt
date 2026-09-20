package com.neosky.servicesupport.presentation.drones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.repository.DroneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DroneListUiState(
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DroneListViewModel @Inject constructor(
    private val droneRepository: DroneRepository,
) : ViewModel() {

    val drones: StateFlow<List<Drone>> = droneRepository.observeDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(DroneListUiState())
    val uiState: StateFlow<DroneListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            when (val result = droneRepository.refreshDrones()) {
                is NetworkResult.Success -> _uiState.value = DroneListUiState(isRefreshing = false)
                is NetworkResult.Error -> _uiState.value = DroneListUiState(isRefreshing = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

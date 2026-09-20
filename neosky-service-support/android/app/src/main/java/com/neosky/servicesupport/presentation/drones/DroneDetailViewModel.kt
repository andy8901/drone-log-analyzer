package com.neosky.servicesupport.presentation.drones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.ServiceRecord
import com.neosky.servicesupport.domain.repository.DroneRepository
import com.neosky.servicesupport.domain.repository.FlightRepository
import com.neosky.servicesupport.domain.repository.ServiceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DroneDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val serviceHistory: List<ServiceRecord> = emptyList(),
    val isServiceHistoryLoading: Boolean = false,
)

@HiltViewModel
class DroneDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val droneRepository: DroneRepository,
    private val flightRepository: FlightRepository,
    private val serviceHistoryRepository: ServiceHistoryRepository,
) : ViewModel() {

    val droneId: String = checkNotNull(savedStateHandle["droneId"])

    val drone: StateFlow<Drone?> = droneRepository.observeDrone(droneId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val flights: StateFlow<List<FlightLog>> = flightRepository.observeFlights(droneId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(DroneDetailUiState())
    val uiState: StateFlow<DroneDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadServiceHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = droneRepository.refreshDrone(droneId)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
            flightRepository.refreshFlights(droneId = droneId)
        }
    }

    fun loadServiceHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isServiceHistoryLoading = true)
            when (val result = serviceHistoryRepository.getServiceHistory(droneId)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isServiceHistoryLoading = false, serviceHistory = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isServiceHistoryLoading = false)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

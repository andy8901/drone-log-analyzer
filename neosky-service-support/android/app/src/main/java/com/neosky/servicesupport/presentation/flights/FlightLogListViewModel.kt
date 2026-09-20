package com.neosky.servicesupport.presentation.flights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.FlightStats
import com.neosky.servicesupport.domain.repository.FlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlightLogListUiState(
    val isRefreshing: Boolean = false,
    val stats: FlightStats? = null,
    val error: String? = null,
)

@HiltViewModel
class FlightLogListViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
) : ViewModel() {

    val flights: StateFlow<List<FlightLog>> = flightRepository.observeFlights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(FlightLogListUiState())
    val uiState: StateFlow<FlightLogListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            flightRepository.refreshFlights()
            when (val statsResult = flightRepository.getStats()) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isRefreshing = false, stats = statsResult.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isRefreshing = false, error = statsResult.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

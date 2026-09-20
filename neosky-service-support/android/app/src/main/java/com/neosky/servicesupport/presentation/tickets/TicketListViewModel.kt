package com.neosky.servicesupport.presentation.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.model.TicketStatus
import com.neosky.servicesupport.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TicketFilter { ALL, OPEN, CLOSED }

data class TicketListUiState(
    val isRefreshing: Boolean = false,
    val filter: TicketFilter = TicketFilter.ALL,
    val error: String? = null,
)

@HiltViewModel
class TicketListViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val allTickets: StateFlow<List<Ticket>> = ticketRepository.observeTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(TicketListUiState())
    val uiState: StateFlow<TicketListUiState> = _uiState.asStateFlow()

    val visibleTickets: StateFlow<List<Ticket>> = combine(allTickets, _uiState) { tickets, ui ->
        when (ui.filter) {
            TicketFilter.ALL -> tickets
            TicketFilter.OPEN -> tickets.filter { it.status != TicketStatus.CLOSED }
            TicketFilter.CLOSED -> tickets.filter { it.status == TicketStatus.CLOSED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun setFilter(filter: TicketFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            when (val result = ticketRepository.refreshTickets()) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isRefreshing = false)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isRefreshing = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

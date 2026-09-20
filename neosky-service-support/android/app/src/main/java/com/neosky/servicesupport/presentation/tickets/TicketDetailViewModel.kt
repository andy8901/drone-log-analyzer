package com.neosky.servicesupport.presentation.tickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUiState(
    val isLoading: Boolean = true,
    val commentText: String = "",
    val isSubmittingComment: Boolean = false,
    val isClosing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val ticketId: String = checkNotNull(savedStateHandle["ticketId"])

    val ticket: StateFlow<Ticket?> = ticketRepository.observeTicket(ticketId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = ticketRepository.refreshTicket(ticketId)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun onCommentChanged(value: String) {
        _uiState.value = _uiState.value.copy(commentText = value)
    }

    fun submitComment() {
        val comment = _uiState.value.commentText.trim()
        if (comment.isEmpty() || _uiState.value.isSubmittingComment) return
        _uiState.value = _uiState.value.copy(isSubmittingComment = true)
        viewModelScope.launch {
            when (val result = ticketRepository.addComment(ticketId, comment)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isSubmittingComment = false, commentText = "")
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isSubmittingComment = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun confirmResolutionAndClose(feedback: String?) {
        if (_uiState.value.isClosing) return
        _uiState.value = _uiState.value.copy(isClosing = true)
        viewModelScope.launch {
            when (val result = ticketRepository.closeTicket(ticketId, feedback)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isClosing = false)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isClosing = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

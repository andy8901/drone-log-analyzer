package com.neosky.servicesupport.presentation.documents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Document
import com.neosky.servicesupport.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val documents: List<Document> = emptyList(),
    val error: String? = null,
)

/**
 * Serves both the top-level Documents screen (reachable from Profile, no scope) and the
 * "Documents" tab inside DroneDetailScreen (scoped to that drone) — [droneId] is read from
 * whichever NavBackStackEntry hosts this composable, and is simply absent for the top-level
 * route.
 */
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val droneId: String? = savedStateHandle["droneId"]
    private val ticketId: String? = savedStateHandle["ticketId"]

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = documentRepository.getDocuments(droneId = droneId, ticketId = ticketId)) {
                is NetworkResult.Success -> _uiState.value = DocumentsUiState(isLoading = false, documents = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

package com.neosky.servicesupport.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.SearchResults
import com.neosky.servicesupport.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: SearchResults? = null,
    val hasSearched: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            when (val result = searchRepository.search(query)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(isSearching = false, results = result.data, hasSearched = true)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isSearching = false, hasSearched = true, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

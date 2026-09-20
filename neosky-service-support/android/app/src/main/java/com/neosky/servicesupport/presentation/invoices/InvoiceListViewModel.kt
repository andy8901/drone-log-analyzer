package com.neosky.servicesupport.presentation.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.domain.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceListUiState(
    val isLoading: Boolean = true,
    val invoices: List<Invoice> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = invoiceRepository.getInvoices()) {
                is NetworkResult.Success -> _uiState.value = InvoiceListUiState(isLoading = false, invoices = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }
}

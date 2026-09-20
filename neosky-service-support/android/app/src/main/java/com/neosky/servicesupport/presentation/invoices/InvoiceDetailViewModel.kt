package com.neosky.servicesupport.presentation.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.domain.repository.InvoiceRepository
import com.neosky.servicesupport.domain.usecase.DownloadInvoicePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceDetailUiState(
    val isLoading: Boolean = true,
    val invoice: Invoice? = null,
    val isDownloading: Boolean = false,
    val error: String? = null,
)

sealed class InvoiceDetailEvent {
    data class DownloadSucceeded(val fileName: String) : InvoiceDetailEvent()
    data class DownloadFailed(val message: String) : InvoiceDetailEvent()
}

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invoiceRepository: InvoiceRepository,
    private val downloadInvoicePdfUseCase: DownloadInvoicePdfUseCase,
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _uiState = MutableStateFlow(InvoiceDetailUiState())
    val uiState: StateFlow<InvoiceDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InvoiceDetailEvent>()
    val events: SharedFlow<InvoiceDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = invoiceRepository.getInvoice(invoiceId)) {
                is NetworkResult.Success -> _uiState.value = InvoiceDetailUiState(isLoading = false, invoice = result.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.apiException.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun downloadPdf() {
        val invoice = _uiState.value.invoice ?: return
        if (_uiState.value.isDownloading) return
        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            when (val result = downloadInvoicePdfUseCase(invoiceId, invoice.invoiceNumber)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isDownloading = false)
                    _events.emit(InvoiceDetailEvent.DownloadSucceeded(result.data))
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isDownloading = false)
                    _events.emit(InvoiceDetailEvent.DownloadFailed(result.apiException.message))
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}

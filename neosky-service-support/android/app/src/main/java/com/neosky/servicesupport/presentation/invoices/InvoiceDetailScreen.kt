package com.neosky.servicesupport.presentation.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.PrimaryButton
import com.neosky.servicesupport.presentation.common.StatusChip
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is InvoiceDetailEvent.DownloadSucceeded -> "Saved ${event.fileName} to Downloads"
                is InvoiceDetailEvent.DownloadFailed -> event.message
            }
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.invoice == null && state.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            state.invoice == null -> Text(state.error ?: "Invoice not found", modifier = Modifier.padding(padding).padding(16.dp))
            else -> InvoiceContent(state.invoice!!, state.isDownloading, onDownload = viewModel::downloadPdf, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun InvoiceContent(invoice: Invoice, isDownloading: Boolean, onDownload: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleLarge)
                        StatusChip(label = invoice.paymentStatus.displayName, tone = StatusColorMapper.toneFor(invoice.paymentStatus))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(invoice.productService, style = MaterialTheme.typography.bodyMedium)
                    InfoRow("Invoice date", DateTimeFormatters.formatDate(invoice.invoiceDate))
                    invoice.droneName?.let { InfoRow("Drone", it) }
                    InfoRow("Coverage", invoice.warrantyType.displayName)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(invoice.items, key = { it.id }) { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.description} ×${item.quantity.toInt()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(currency(item.amount), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Subtotal", currency(invoice.subtotalAmount))
            InfoRow("GST", currency(invoice.gstAmount))
            InfoRow("Total", currency(invoice.totalAmount))
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "Download PDF",
                onClick = onDownload,
                isLoading = isDownloading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun currency(amount: Double) = String.format(Locale.getDefault(), "₹%,.2f", amount)

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

package com.neosky.servicesupport.presentation.invoices

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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.StatusChip
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onInvoiceClick: (String) -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Invoices") }) }) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (state.invoices.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = "No invoices yet",
                    subtitle = state.error ?: "Invoices for services and purchases will appear here.",
                    icon = Icons.Filled.Receipt,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceCard(invoice, onClick = { onInvoiceClick(invoice.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleSmall)
                StatusChip(label = invoice.paymentStatus.displayName, tone = StatusColorMapper.toneFor(invoice.paymentStatus))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(invoice.productService, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(DateTimeFormatters.formatDate(invoice.invoiceDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.getDefault(), "₹%,.2f", invoice.totalAmount), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

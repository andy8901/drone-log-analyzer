package com.neosky.servicesupport.presentation.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onDroneClick: (String) -> Unit,
    onTicketClick: (String) -> Unit,
    onInvoiceClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text("Search drones, tickets, invoices…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            when {
                state.isSearching -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                !state.hasSearched -> EmptyState(title = "Search across your account", subtitle = "Try a drone serial number, ticket number or invoice number.")
                state.results == null || state.results!!.let { it.drones.isEmpty() && it.tickets.isEmpty() && it.invoices.isEmpty() && it.serviceRecords.isEmpty() } ->
                    EmptyState(title = "No results", subtitle = state.error ?: "Try a different search term.")
                else -> ResultsList(state.results!!, onDroneClick, onTicketClick, onInvoiceClick)
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: com.neosky.servicesupport.domain.model.SearchResults,
    onDroneClick: (String) -> Unit,
    onTicketClick: (String) -> Unit,
    onInvoiceClick: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
        if (results.drones.isNotEmpty()) {
            item { SectionHeader("Drones", modifier = Modifier.padding(horizontal = 0.dp)) }
            items(results.drones, key = { "drone-" + it.id }) { drone ->
                ResultRow(drone.droneName, drone.serialNumber) { onDroneClick(drone.id) }
            }
        }
        if (results.tickets.isNotEmpty()) {
            item { SectionHeader("Tickets", modifier = Modifier.padding(horizontal = 0.dp)) }
            items(results.tickets, key = { "ticket-" + it.id }) { ticket ->
                ResultRow(ticket.ticketNumber, ticket.subject) { onTicketClick(ticket.id) }
            }
        }
        if (results.invoices.isNotEmpty()) {
            item { SectionHeader("Invoices", modifier = Modifier.padding(horizontal = 0.dp)) }
            items(results.invoices, key = { "invoice-" + it.id }) { invoice ->
                ResultRow(invoice.invoiceNumber, invoice.productService) { onInvoiceClick(invoice.id) }
            }
        }
        if (results.serviceRecords.isNotEmpty()) {
            item { SectionHeader("Service Records", modifier = Modifier.padding(horizontal = 0.dp)) }
            items(results.serviceRecords, key = { "service-" + it.id }) { record ->
                ResultRow(record.serviceDate.toString(), record.issueSummary ?: "") {}
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

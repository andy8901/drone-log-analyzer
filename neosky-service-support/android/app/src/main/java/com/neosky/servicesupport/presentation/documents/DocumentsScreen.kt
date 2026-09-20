package com.neosky.servicesupport.presentation.documents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.neosky.servicesupport.domain.model.Document
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.SectionHeader

/** Top-level usage takes [onBack]; as a drone-detail tab, pass onBack = null to hide the app bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        when {
            state.isLoading && state.documents.isEmpty() -> LoadingIndicator(modifier = contentModifier)
            state.documents.isEmpty() -> EmptyState(
                title = "No documents yet",
                subtitle = state.error ?: "Warranty certificates, service reports and invoices will appear here.",
                icon = Icons.Filled.Description,
                modifier = contentModifier,
            )
            else -> DocumentsList(state.documents, contentModifier)
        }
    }

    if (onBack != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Documents") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding -> content(modifier.padding(padding)) }
    } else {
        content(modifier.fillMaxSize())
    }
}

@Composable
private fun DocumentsList(documents: List<Document>, modifier: Modifier) {
    val grouped = documents.groupBy { it.documentType }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        grouped.forEach { (type, docs) ->
            item { SectionHeader(type.displayName, modifier = Modifier.padding(horizontal = 0.dp)) }
            items(docs, key = { it.id }) { doc -> DocumentRow(doc) }
        }
    }
}

@Composable
private fun DocumentRow(document: Document) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp).fillMaxWidth()) {
                Text(document.fileName, style = MaterialTheme.typography.bodyMedium)
                val linkedTo = document.droneName ?: document.ticketNumber
                Text(
                    listOfNotNull(linkedTo, DateTimeFormatters.formatDate(document.createdAt)).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

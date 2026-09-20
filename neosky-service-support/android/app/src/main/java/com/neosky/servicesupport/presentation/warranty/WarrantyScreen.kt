package com.neosky.servicesupport.presentation.warranty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.Warranty
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.SectionHeader
import com.neosky.servicesupport.presentation.common.StatusChip

@Composable
fun WarrantyScreen(
    modifier: Modifier = Modifier,
    viewModel: WarrantyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading && state.warranty == null -> LoadingIndicator(modifier = modifier)
        state.warranty == null -> EmptyState(
            title = "No warranty information",
            subtitle = state.error ?: "This drone has no warranty record yet.",
            icon = Icons.Filled.Shield,
            modifier = modifier,
        )
        else -> WarrantyContent(state.warranty!!, modifier)
    }
}

@Composable
private fun WarrantyContent(warranty: Warranty, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Warranty Status", style = MaterialTheme.typography.titleMedium)
                    StatusChip(label = warranty.status.displayName, tone = StatusColorMapper.toneFor(warranty.status))
                }
                Spacer(modifier = Modifier.height(12.dp))
                LabelValueRow("Start date", DateTimeFormatters.formatDate(warranty.startDate))
                LabelValueRow("End date", DateTimeFormatters.formatDate(warranty.endDate))
                LabelValueRow("Days remaining", DateTimeFormatters.formatDaysRemaining(warranty.daysRemaining))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Covered Items", modifier = Modifier.padding(horizontal = 0.dp))
        ItemList(items = warranty.coveredItems, icon = Icons.Filled.CheckCircle, tint = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Excluded Items", modifier = Modifier.padding(horizontal = 0.dp))
        ItemList(items = warranty.excludedItems, icon = Icons.Filled.Cancel, tint = MaterialTheme.colorScheme.error)

        if (warranty.claims.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Claims", modifier = Modifier.padding(horizontal = 0.dp))
            warranty.claims.forEach { claim ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(claim.claimNumber, style = MaterialTheme.typography.bodyMedium)
                        StatusChip(label = claim.status.displayName, tone = StatusColorMapper.toneFor(claim.status))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemList(items: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Column {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.height(18.dp))
                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (items.isEmpty()) {
            Text("None listed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

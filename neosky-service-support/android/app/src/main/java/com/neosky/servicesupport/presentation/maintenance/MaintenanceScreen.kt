package com.neosky.servicesupport.presentation.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
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
import com.neosky.servicesupport.domain.model.MaintenanceInfo
import com.neosky.servicesupport.domain.model.MaintenanceSchedule
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.SectionHeader
import com.neosky.servicesupport.presentation.common.StatusChip

@Composable
fun MaintenanceScreen(
    modifier: Modifier = Modifier,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading && state.info == null -> LoadingIndicator(modifier = modifier)
        state.info == null -> EmptyState(
            title = "No maintenance information",
            subtitle = state.error ?: "This drone has no maintenance schedule yet.",
            icon = Icons.Filled.Build,
            modifier = modifier,
        )
        else -> MaintenanceContent(state.info!!, modifier)
    }
}

@Composable
private fun MaintenanceContent(info: MaintenanceInfo, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        info.schedule?.let { schedule -> ScheduleCard(schedule) }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Maintenance History", modifier = Modifier.padding(horizontal = 0.dp))
        if (info.records.isEmpty()) {
            Text(
                "No maintenance events recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            info.records.forEach { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.maintenanceType, style = MaterialTheme.typography.bodyMedium)
                            Text(DateTimeFormatters.formatDate(record.performedAt), style = MaterialTheme.typography.bodySmall)
                        }
                        if (!record.description.isNullOrBlank()) {
                            Text(
                                record.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (record.partsReplaced.isNotEmpty()) {
                            Text(
                                "Parts replaced: ${record.partsReplaced.joinToString()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: MaintenanceSchedule) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Maintenance Status", style = MaterialTheme.typography.titleMedium)
                StatusChip(label = schedule.status.displayName, tone = StatusColorMapper.toneFor(schedule.status))
            }
            Spacer(modifier = Modifier.height(12.dp))
            LabelValueRow("Last maintenance", DateTimeFormatters.formatDate(schedule.lastMaintenanceDate))
            LabelValueRow("Interval", intervalLabel(schedule))
            LabelValueRow("Current flight hours", DateTimeFormatters.formatHours(schedule.currentFlightHours))
            schedule.nextDueHours?.let { LabelValueRow("Next due at", DateTimeFormatters.formatHours(it)) }
            schedule.remainingHours?.let { LabelValueRow("Remaining", DateTimeFormatters.formatHours(it)) }
            schedule.nextDueDate?.let { LabelValueRow("Next due date", DateTimeFormatters.formatDate(it)) }
        }
    }
}

private fun intervalLabel(schedule: MaintenanceSchedule): String {
    val parts = mutableListOf<String>()
    schedule.intervalFlightHours?.let { parts.add("${it.toInt()} flight hrs") }
    schedule.intervalCalendarDays?.let { parts.add("${it} days") }
    return if (parts.isEmpty()) "—" else parts.joinToString(" / ")
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

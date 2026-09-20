package com.neosky.servicesupport.presentation.flights

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.FlightStats
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightLogListScreen(
    onAddFlight: () -> Unit,
    viewModel: FlightLogListViewModel = hiltViewModel(),
) {
    val flights by viewModel.flights.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Flight Log") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFlight) {
                Icon(Icons.Filled.Add, contentDescription = "Log a new flight")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (flights.isEmpty() && !uiState.isRefreshing) {
                EmptyState(
                    title = "No flights logged yet",
                    subtitle = uiState.error ?: "Log your first flight to start tracking hours.",
                    icon = Icons.Filled.FlightTakeoff,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    uiState.stats?.let { stats ->
                        item { StatsSection(stats) }
                    }
                    item { SectionHeader("Flight History") }
                    items(flights, key = { it.id }) { flight ->
                        FlightRow(flight, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(stats: FlightStats) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stats.totalFlights.toString(), style = MaterialTheme.typography.headlineSmall)
                Text("Total Flights", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text(DateTimeFormatters.formatHours(stats.totalFlightHours), style = MaterialTheme.typography.headlineSmall)
                Text("Total Hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text(DateTimeFormatters.formatDurationMinutes(stats.averageDurationMinutes), style = MaterialTheme.typography.headlineSmall)
                Text("Avg Duration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (stats.monthlyFlightHours.isNotEmpty()) {
            SectionHeader("Flight Hours / Month")
            SimpleBarChart(
                data = stats.monthlyFlightHours.map { DateTimeFormatters.formatMonthBucket(it.month) to it.hours.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                valueFormatter = { "%.0fh".format(it) },
            )
        }
        if (stats.hoursPerDrone.isNotEmpty()) {
            SectionHeader("Flight Hours / Drone")
            SimpleBarChart(
                data = stats.hoursPerDrone.map { it.droneName to it.hours.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                valueFormatter = { "%.0fh".format(it) },
            )
        }
    }
}

@Composable
private fun FlightRow(flight: FlightLog, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(flight.droneName ?: "Drone", style = MaterialTheme.typography.bodyMedium)
                Text(DateTimeFormatters.formatDurationMinutes(flight.durationMinutes), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "${DateTimeFormatters.formatDate(flight.flightDate)} • ${flight.location ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (flight.syncStatus == com.neosky.servicesupport.domain.model.SyncStatus.PENDING_SYNC) {
                Text("Pending sync…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

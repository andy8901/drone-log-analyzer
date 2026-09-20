package com.neosky.servicesupport.presentation.drones

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.ServiceRecord
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.StatusChip
import com.neosky.servicesupport.presentation.documents.DocumentsScreen
import com.neosky.servicesupport.presentation.maintenance.MaintenanceScreen
import com.neosky.servicesupport.presentation.warranty.WarrantyScreen

private val tabTitles = listOf("Overview", "Flight History", "Warranty", "Maintenance", "Service History", "Documents")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneDetailScreen(
    droneId: String,
    onBack: () -> Unit,
    onRaiseTicket: (String) -> Unit,
    onLogFlight: (String) -> Unit,
    viewModel: DroneDetailViewModel = hiltViewModel(),
) {
    val drone by viewModel.drone.collectAsState()
    val flights by viewModel.flights.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(drone?.droneName ?: "Drone") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onRaiseTicket(droneId) }) {
                Icon(Icons.Filled.Assignment, contentDescription = "Raise a ticket for this drone")
            }
        },
    ) { padding ->
        when {
            drone == null && uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            drone == null -> EmptyState(
                title = "Couldn't load this drone",
                subtitle = uiState.error,
                icon = Icons.Filled.FlightTakeoff,
                modifier = Modifier.padding(padding),
            )
            else -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }
                when (selectedTab) {
                    0 -> OverviewTab(drone!!, onLogFlight, Modifier.fillMaxSize())
                    1 -> FlightHistoryTab(flights, Modifier.fillMaxSize())
                    2 -> WarrantyScreen(modifier = Modifier.fillMaxSize())
                    3 -> MaintenanceScreen(modifier = Modifier.fillMaxSize())
                    4 -> ServiceHistoryTab(uiState.serviceHistory, uiState.isServiceHistoryLoading, Modifier.fillMaxSize())
                    5 -> DocumentsScreen(onBack = null, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(drone: Drone, onLogFlight: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(drone.droneName, style = MaterialTheme.typography.titleLarge)
                        StatusChip(label = drone.status.displayName, tone = StatusColorMapper.toneFor(drone.status))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Model", drone.model)
                    InfoRow("Serial number", drone.serialNumber)
                    drone.uin?.let { InfoRow("UIN", it) }
                    drone.firmwareVersion?.let { InfoRow("Firmware", it) }
                    InfoRow("Purchase date", DateTimeFormatters.formatDate(drone.purchaseDate))
                    InfoRow("Total flight hours", DateTimeFormatters.formatHours(drone.totalFlightHours))
                    InfoRow("Total flights", drone.totalFlights.toString())
                    InfoRow("Last flight", DateTimeFormatters.formatDateTime(drone.lastFlightAt))
                }
            }
        }
        if (drone.components.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)); Text("Components", style = MaterialTheme.typography.titleMedium) }
            items(drone.components, key = { it.id }) { component ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(component.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${component.componentType.name.lowercase().replace('_', ' ')} • ${component.serialNumber ?: "—"} • ${component.cycleCount} cycles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun FlightHistoryTab(flights: List<FlightLog>, modifier: Modifier) {
    if (flights.isEmpty()) {
        EmptyState(title = "No flights logged yet", subtitle = "Flights for this drone will appear here.", icon = Icons.Filled.FlightTakeoff, modifier = modifier)
        return
    }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        items(flights, key = { it.id }) { flight ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(DateTimeFormatters.formatDate(flight.flightDate), style = MaterialTheme.typography.bodyMedium)
                        Text(DateTimeFormatters.formatDurationMinutes(flight.durationMinutes), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        listOfNotNull(flight.location, flight.missionType).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceHistoryTab(records: List<ServiceRecord>, isLoading: Boolean, modifier: Modifier) {
    if (records.isEmpty()) {
        if (isLoading) {
            LoadingIndicator(modifier = modifier)
        } else {
            EmptyState(title = "No service history yet", subtitle = "Completed services for this drone will appear here.", modifier = modifier)
        }
        return
    }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        items(records, key = { it.id }) { record ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(DateTimeFormatters.formatDate(record.serviceDate), style = MaterialTheme.typography.bodyMedium)
                        Text(record.status, style = MaterialTheme.typography.bodySmall)
                    }
                    record.issueSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                    record.actionTaken?.let {
                        Text(
                            "Action: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

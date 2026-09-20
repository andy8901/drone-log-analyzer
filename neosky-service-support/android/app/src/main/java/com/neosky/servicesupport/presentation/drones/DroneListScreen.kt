package com.neosky.servicesupport.presentation.drones

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
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneListScreen(
    onDroneClick: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: DroneListViewModel = hiltViewModel(),
) {
    val drones by viewModel.drones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Drones") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (drones.isEmpty() && !uiState.isRefreshing) {
                EmptyState(
                    title = "No drones registered yet",
                    subtitle = uiState.error ?: "Your registered NeoSky/TAS drones will appear here.",
                    icon = Icons.Filled.FlightTakeoff,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(drones, key = { it.id }) { drone ->
                        DroneCard(drone = drone, onClick = { onDroneClick(drone.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DroneCard(drone: Drone, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(drone.droneName, style = MaterialTheme.typography.titleMedium)
                StatusChip(label = drone.status.displayName, tone = StatusColorMapper.toneFor(drone.status))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${drone.model} • ${drone.serialNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(DateTimeFormatters.formatHours(drone.totalFlightHours), style = MaterialTheme.typography.bodySmall)
                Text("${drone.totalFlights} flights", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

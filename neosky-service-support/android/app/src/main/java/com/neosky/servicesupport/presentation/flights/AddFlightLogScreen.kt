package com.neosky.servicesupport.presentation.flights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.domain.model.FlightResult
import com.neosky.servicesupport.presentation.common.DateTimePickerField
import com.neosky.servicesupport.presentation.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlightLogScreen(
    preselectedDroneId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddFlightLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val drones by viewModel.drones.collectAsState()
    val durationPreview by viewModel.durationPreviewMinutes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved() }
    }
    LaunchedEffect(preselectedDroneId) {
        if (preselectedDroneId != null && state.droneId == null) viewModel.onDroneSelected(preselectedDroneId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log a Flight") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            var droneMenuExpanded by remember { mutableStateOf(false) }
            val selectedDrone = drones.firstOrNull { it.id == state.droneId }
            ExposedDropdownMenuBox(expanded = droneMenuExpanded, onExpandedChange = { droneMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedDrone?.droneName ?: "Select a drone",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Drone") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = droneMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                androidx.compose.material3.ExposedDropdownMenu(expanded = droneMenuExpanded, onDismissRequest = { droneMenuExpanded = false }) {
                    drones.forEach { drone ->
                        DropdownMenuItem(text = { Text(drone.droneName) }, onClick = {
                            viewModel.onDroneSelected(drone.id)
                            droneMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DateTimePickerField(label = "Start time", value = state.startTime, onValueChange = viewModel::onStartTimeSelected, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            DateTimePickerField(label = "End time", value = state.endTime, onValueChange = viewModel::onEndTimeSelected, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duration: " + (durationPreview?.let { DateTimeFormatters.formatDurationMinutes(it) } ?: "—"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.manualDurationOverride,
                onValueChange = viewModel::onManualDurationChanged,
                label = { Text("Override duration in minutes (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = state.location, onValueChange = viewModel::onLocationChanged, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.maxAltitudeM,
                    onValueChange = viewModel::onMaxAltitudeChanged,
                    label = { Text("Max altitude (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.distanceTravelledKm,
                    onValueChange = viewModel::onDistanceChanged,
                    label = { Text("Distance (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = state.missionType, onValueChange = viewModel::onMissionTypeChanged, label = { Text("Mission type") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = state.payloadUsed, onValueChange = viewModel::onPayloadUsedChanged, label = { Text("Payload used") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.batteryUsed,
                    onValueChange = viewModel::onBatteryUsedChanged,
                    label = { Text("Battery ID") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.batteryCycle,
                    onValueChange = viewModel::onBatteryCycleChanged,
                    label = { Text("Battery cycle") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = state.weather, onValueChange = viewModel::onWeatherChanged, label = { Text("Weather") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))
            var resultMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = resultMenuExpanded, onExpandedChange = { resultMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.flightResult.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Flight result") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                androidx.compose.material3.ExposedDropdownMenu(expanded = resultMenuExpanded, onDismissRequest = { resultMenuExpanded = false }) {
                    FlightResult.entries.filter { it != FlightResult.UNKNOWN }.forEach { result ->
                        DropdownMenuItem(text = { Text(result.displayName) }, onClick = {
                            viewModel.onFlightResultSelected(result)
                            resultMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = state.remarks, onValueChange = viewModel::onRemarksChanged, label = { Text("Remarks") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Checkbox(checked = state.incidentFlag, onCheckedChange = viewModel::onIncidentFlagChanged)
                Text("Flag an incident on this flight")
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "Save Flight",
                onClick = viewModel::save,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Saved instantly, even offline — it will sync automatically once you're back online.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

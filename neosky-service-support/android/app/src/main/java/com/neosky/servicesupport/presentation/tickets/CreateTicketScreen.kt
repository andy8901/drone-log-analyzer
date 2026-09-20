package com.neosky.servicesupport.presentation.tickets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
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
import com.neosky.servicesupport.domain.model.TicketCategory
import com.neosky.servicesupport.domain.model.TicketPriority
import com.neosky.servicesupport.presentation.common.DateTimePickerField
import com.neosky.servicesupport.presentation.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    preselectedDroneId: String?,
    onSubmitted: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateTicketViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val drones by viewModel.drones.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.submitted.collect { ticketId -> onSubmitted(ticketId) }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, "photo.jpg", "photo") }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, "video.mp4", "video") }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, "document.pdf", "document") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raise a Ticket") },
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
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                androidx.compose.material3.ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                    TicketCategory.entries.filter { it != TicketCategory.UNKNOWN }.forEach { category ->
                        DropdownMenuItem(text = { Text(category.displayName) }, onClick = {
                            viewModel.onCategorySelected(category)
                            categoryMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.subCategory,
                onValueChange = viewModel::onSubCategoryChanged,
                label = { Text("Sub-category (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                TicketPriority.entries.filter { it != TicketPriority.UNKNOWN }.forEach { priority ->
                    AssistChip(
                        onClick = { viewModel.onPrioritySelected(priority) },
                        label = { Text(priority.displayName) },
                        colors = if (state.priority == priority) {
                            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        } else {
                            androidx.compose.material3.AssistChipDefaults.assistChipColors()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.subject,
                onValueChange = viewModel::onSubjectChanged,
                label = { Text("Subject") },
                isError = state.fieldErrors.containsKey("subject"),
                supportingText = { state.fieldErrors["subject"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description") },
                minLines = 3,
                isError = state.fieldErrors.containsKey("description"),
                supportingText = { state.fieldErrors["description"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            DateTimePickerField(
                label = "Issue date & time",
                value = state.issueDatetime,
                onValueChange = viewModel::onIssueDatetimeSelected,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChanged,
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.flightHoursAtIssue,
                onValueChange = viewModel::onFlightHoursChanged,
                label = { Text("Flight hours at time of issue (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Attachments", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                AssistChip(onClick = { photoLauncher.launch("image/*") }, label = { Text("Add Photo") }, leadingIcon = { Icon(Icons.Filled.AttachFile, null) })
                AssistChip(onClick = { videoLauncher.launch("video/*") }, label = { Text("Add Video") }, leadingIcon = { Icon(Icons.Filled.AttachFile, null) })
                AssistChip(onClick = { documentLauncher.launch("application/pdf") }, label = { Text("Add Document") }, leadingIcon = { Icon(Icons.Filled.AttachFile, null) })
            }
            state.attachments.forEach { attachment ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(attachment.displayName, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
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
                text = "Submit Ticket",
                onClick = viewModel::submit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    LaunchedEffect(preselectedDroneId) {
        if (preselectedDroneId != null && state.droneId == null) {
            viewModel.onDroneSelected(preselectedDroneId)
        }
    }
}

package com.neosky.servicesupport.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onNotifications: () -> Unit,
    onDocuments: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (!state.isEditing && state.customer != null) {
                        IconButton(onClick = viewModel::startEditing) { Icon(Icons.Filled.Edit, contentDescription = "Edit profile") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.customer == null && state.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                state.customer?.let { customer ->
                    Text(customer.fullName, style = MaterialTheme.typography.headlineSmall)
                    Text(customer.customerCode, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isEditing) {
                        EditForm(state, viewModel)
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                InfoRow("Email", customer.email)
                                InfoRow("Phone", customer.phone ?: "—")
                                InfoRow("Company", customer.companyName ?: "—")
                                InfoRow("Billing address", customer.billingAddress ?: "—")
                                InfoRow("GSTIN", customer.gstin ?: "—")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                ListItem(
                    headlineContent = { Text("Notifications") },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) { Text("Open Notifications") }
                TextButton(onClick = onDocuments, modifier = Modifier.fillMaxWidth()) { Text("Open Documents") }

                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Log out")
                }
            }
        }
    }
}

@Composable
private fun EditForm(state: ProfileUiState, viewModel: ProfileViewModel) {
    OutlinedTextField(value = state.editFullName, onValueChange = viewModel::onFullNameChanged, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = state.editPhone, onValueChange = viewModel::onPhoneChanged, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = state.editCompanyName, onValueChange = viewModel::onCompanyNameChanged, label = { Text("Company name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = state.editBillingAddress, onValueChange = viewModel::onBillingAddressChanged, label = { Text("Billing address") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = state.editGstin, onValueChange = viewModel::onGstinChanged, label = { Text("GSTIN") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = viewModel::cancelEditing) { Text("Cancel") }
        PrimaryButton(text = "Save", onClick = viewModel::saveProfile, isLoading = state.isSaving, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

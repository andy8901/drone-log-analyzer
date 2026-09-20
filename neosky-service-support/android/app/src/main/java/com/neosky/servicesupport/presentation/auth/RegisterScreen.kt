package com.neosky.servicesupport.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.presentation.common.PrimaryButton

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.registrationComplete.collect { onRegisterSuccess() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Create your account", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Register to track your NeoSky drones, tickets and service history",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        when (state.step) {
            RegisterStep.FORM -> RegisterForm(state, viewModel)
            RegisterStep.OTP -> OtpForm(state, viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row2(onNavigateToLogin)
    }
}

@Composable
private fun RegisterForm(state: RegisterUiState, viewModel: RegisterViewModel) {
    OutlinedTextField(
        value = state.fullName,
        onValueChange = { viewModel.onFieldChanged(fullName = it) },
        label = { Text("Full name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.email,
        onValueChange = { viewModel.onFieldChanged(email = it) },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.fieldErrors.containsKey("email"),
        supportingText = { state.fieldErrors["email"]?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.phone,
        onValueChange = { viewModel.onFieldChanged(phone = it) },
        label = { Text("Phone") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        isError = state.fieldErrors.containsKey("phone"),
        supportingText = { state.fieldErrors["phone"]?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.companyName,
        onValueChange = { viewModel.onFieldChanged(companyName = it) },
        label = { Text("Company name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = { viewModel.onFieldChanged(password = it) },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.confirmPassword,
        onValueChange = { viewModel.onFieldChanged(confirmPassword = it) },
        label = { Text("Confirm password") },
        singleLine = true,
        isError = state.fieldErrors.containsKey("confirmPassword"),
        supportingText = { state.fieldErrors["confirmPassword"]?.let { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.generalError != null) {
        Text(
            text = state.generalError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    PrimaryButton(
        text = "Create account",
        onClick = viewModel::submitRegistration,
        isLoading = state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OtpForm(state: RegisterUiState, viewModel: RegisterViewModel) {
    if (state.infoMessage != null) {
        Text(state.infoMessage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
    }
    OutlinedTextField(
        value = state.otp,
        onValueChange = viewModel::onOtpChanged,
        label = { Text("Verification code") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.generalError != null) {
        Text(
            text = state.generalError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    PrimaryButton(
        text = "Verify",
        onClick = viewModel::verifyOtp,
        isLoading = state.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Row2(onNavigateToLogin: () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onNavigateToLogin) { Text("Sign in") }
    }
}

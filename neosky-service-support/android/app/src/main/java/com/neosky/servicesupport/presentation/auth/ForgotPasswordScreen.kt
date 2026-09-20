package com.neosky.servicesupport.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.presentation.common.PrimaryButton

@Composable
fun ForgotPasswordScreen(
    onDone: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onDone() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Text(
            "We'll send a verification code to your registered email or phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        if (state.step == ForgotPasswordStep.REQUEST) {
            OutlinedTextField(
                value = state.identifier,
                onValueChange = viewModel::onIdentifierChanged,
                label = { Text("Email or phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton("Send code", onClick = viewModel::requestOtp, isLoading = state.isLoading, modifier = Modifier.fillMaxWidth())
        } else {
            if (state.message != null) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
            }
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::onOtpChanged,
                label = { Text("Verification code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChanged,
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton("Reset password", onClick = viewModel::resetPassword, isLoading = state.isLoading, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) { Text("Back to sign in") }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

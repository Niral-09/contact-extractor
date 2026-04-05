package com.contactextractor.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onConfigured: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showApiKey by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.addOnSuccessListener { account ->
                viewModel.onGoogleSignInSuccess(
                    email = account.email ?: "",
                    account = account.account!!
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Configure the app once before using it.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Gemini API Key
            OutlinedTextField(
                value = uiState.geminiApiKey,
                onValueChange = viewModel::onGeminiKeyChanged,
                label = { Text("Gemini API Key") },
                placeholder = { Text("Enter your Gemini API key") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Google Sign-In
            if (uiState.signedInEmail != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Signed in as: ${uiState.signedInEmail}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope("https://www.googleapis.com/auth/spreadsheets"))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        signInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google (for Sheets access)")
                }
            }

            // Spreadsheet URL/ID
            OutlinedTextField(
                value = uiState.spreadsheetIdInput,
                onValueChange = viewModel::onSpreadsheetInputChanged,
                label = { Text("Google Sheets URL or ID") },
                placeholder = { Text("Paste full URL or spreadsheet ID") },
                singleLine = true,
                isError = uiState.spreadsheetError != null,
                supportingText = uiState.spreadsheetError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = { viewModel.saveSettings(onSuccess = onConfigured) },
                enabled = !uiState.isValidatingSheet
                        && uiState.geminiApiKey.isNotBlank()
                        && uiState.spreadsheetIdInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isValidatingSheet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save & Continue")
                }
            }
        }
    }
}

package com.contactextractor.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.contactextractor.data.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val sheetsState by viewModel.sheetsState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val spreadsheetId = remember {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context, "secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("spreadsheet_id", "") ?: ""
    }

    LaunchedEffect(sheetsState) {
        when (val state = sheetsState) {
            is SheetsUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetSheetsState()
            }
            is SheetsUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetSheetsState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extracted Contacts (${contacts.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportVcf(context) },
                        modifier = Modifier.weight(1f),
                        enabled = contacts.isNotEmpty()
                    ) {
                        Text("Export VCF")
                    }
                    Button(
                        onClick = { viewModel.saveToSheets(spreadsheetId) },
                        modifier = Modifier.weight(1f),
                        enabled = contacts.isNotEmpty() && sheetsState !is SheetsUiState.Saving
                    ) {
                        if (sheetsState is SheetsUiState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save to Sheets")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("No contacts extracted.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { viewModel.addContact() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add manually")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Name", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Mobile", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("City", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(40.dp))
                    }
                    HorizontalDivider()
                }

                itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                    ContactRow(
                        contact = contact,
                        onContactChanged = { viewModel.updateContact(index, it) },
                        onDelete = { viewModel.deleteContact(index) }
                    )
                }

                item {
                    TextButton(
                        onClick = { viewModel.addContact() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Row")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onContactChanged: (Contact) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = contact.name,
            onValueChange = { onContactChanged(contact.copy(name = it)) },
            modifier = Modifier.weight(2f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = contact.mobile,
            onValueChange = { onContactChanged(contact.copy(mobile = it)) },
            modifier = Modifier.weight(2f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = contact.city,
            onValueChange = { onContactChanged(contact.copy(city = it)) },
            modifier = Modifier.weight(2f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

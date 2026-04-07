package com.contactextractor.ui.settings

import android.accounts.Account
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contactextractor.data.prefs.SecurePrefsRepository
import com.contactextractor.data.sheets.GoogleSheetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val geminiApiKey: String = "",
    val spreadsheetIdInput: String = "",
    val signedInEmail: String? = null,
    val spreadsheetError: String? = null,
    val isValidatingSheet: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SecurePrefsRepository,
    private val sheetsRepository: GoogleSheetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            geminiApiKey = prefs.getGeminiApiKey(),
            spreadsheetIdInput = prefs.getSpreadsheetId()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onGeminiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
    }

    fun onSpreadsheetInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(spreadsheetIdInput = input, spreadsheetError = null)
    }

    fun onGoogleSignInSuccess(email: String, account: Account) {
        sheetsRepository.setAccount(account)
        _uiState.value = _uiState.value.copy(signedInEmail = email)
    }

    fun saveSettings(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.geminiApiKey.isBlank()) return
        val spreadsheetId = extractSpreadsheetId(state.spreadsheetIdInput)
        if (spreadsheetId == null) {
            _uiState.value = state.copy(spreadsheetError = "Invalid Sheets URL or ID")
            return
        }
        _uiState.value = state.copy(isValidatingSheet = true, spreadsheetError = null)
        viewModelScope.launch {
            val valid = sheetsRepository.validateSpreadsheetId(spreadsheetId)
            if (valid) {
                prefs.setGeminiApiKey(state.geminiApiKey)
                prefs.setSpreadsheetId(spreadsheetId)
                _uiState.value = _uiState.value.copy(isValidatingSheet = false, isSaved = true)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isValidatingSheet = false,
                    spreadsheetError = "Cannot access this spreadsheet. Check the URL and your sign-in."
                )
            }
        }
    }

    private fun extractSpreadsheetId(input: String): String? {
        if (input.isBlank()) return null
        val urlRegex = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        val match = urlRegex.find(input)
        return match?.groupValues?.get(1) ?: input.trim().takeIf { it.isNotBlank() }
    }
}

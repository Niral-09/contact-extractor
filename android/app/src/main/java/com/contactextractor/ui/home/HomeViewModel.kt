package com.contactextractor.ui.home

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contactextractor.data.model.Contact
import com.contactextractor.data.ocr.GeminiOcrRepository
import com.contactextractor.data.prefs.SecurePrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class HomeUiState {
    object Idle : HomeUiState()
    data class Processing(val retryCountdown: Int? = null) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class Success(val contacts: List<Contact>) : HomeUiState()
}

// Shared state bridge between HomeViewModel and ResultsViewModel
object ContactsHolder {
    var contacts: List<Contact> = emptyList()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ocrRepository: GeminiOcrRepository,
    private val prefs: SecurePrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun processImage(context: Context, imageUri: Uri, onSuccess: () -> Unit) {
        _uiState.value = HomeUiState.Processing()
        viewModelScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                        ?: throw IllegalStateException("Could not read image")
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                val apiKey = prefs.getGeminiApiKey()
                val contacts = ocrRepository.extractContacts(
                    apiKey = apiKey,
                    base64Image = base64,
                    onRetryCountdown = { seconds ->
                        _uiState.value = HomeUiState.Processing(retryCountdown = seconds)
                    }
                )
                ContactsHolder.contacts = contacts
                _uiState.value = HomeUiState.Success(contacts)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unexpected error")
            }
        }
    }

    fun reset() {
        _uiState.value = HomeUiState.Idle
    }
}

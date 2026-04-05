package com.contactextractor.ui.results

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contactextractor.data.model.Contact
import com.contactextractor.data.sheets.GoogleSheetsRepository
import com.contactextractor.ui.home.ContactsHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class SheetsUiState {
    object Idle : SheetsUiState()
    object Saving : SheetsUiState()
    data class Success(val message: String) : SheetsUiState()
    data class Error(val message: String) : SheetsUiState()
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val sheetsRepository: GoogleSheetsRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow(ContactsHolder.contacts.toMutableList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _sheetsState = MutableStateFlow<SheetsUiState>(SheetsUiState.Idle)
    val sheetsState: StateFlow<SheetsUiState> = _sheetsState.asStateFlow()

    fun updateContact(index: Int, updated: Contact) {
        val list = _contacts.value.toMutableList()
        list[index] = updated
        _contacts.value = list
    }

    fun deleteContact(index: Int) {
        val list = _contacts.value.toMutableList()
        list.removeAt(index)
        _contacts.value = list
    }

    fun addContact() {
        val list = _contacts.value.toMutableList()
        list.add(Contact(name = "", mobile = "", city = ""))
        _contacts.value = list
    }

    fun exportVcf(context: Context) {
        val vcfContent = VcfBuilder.build(_contacts.value)
        val file = File(context.cacheDir, "contacts.vcf")
        file.writeText(vcfContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export VCF"))
    }

    fun saveToSheets(spreadsheetId: String) {
        _sheetsState.value = SheetsUiState.Saving
        viewModelScope.launch {
            try {
                sheetsRepository.saveContacts(spreadsheetId, _contacts.value)
                _sheetsState.value = SheetsUiState.Success("Contacts saved to Google Sheets!")
            } catch (e: Exception) {
                _sheetsState.value = SheetsUiState.Error(e.message ?: "Failed to save to Sheets")
            }
        }
    }

    fun resetSheetsState() {
        _sheetsState.value = SheetsUiState.Idle
    }
}

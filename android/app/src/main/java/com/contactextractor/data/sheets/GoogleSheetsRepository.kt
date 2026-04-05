package com.contactextractor.data.sheets

import android.accounts.Account
import android.content.Context
import com.contactextractor.data.model.Contact
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSheetsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var account: Account? = null

    fun setAccount(account: Account) {
        this.account = account
    }

    private fun buildSheetsService(): Sheets {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccount = account
                ?: throw IllegalStateException("Google account not set. Sign in first.")
        }
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Contact Extractor").build()
    }

    suspend fun validateSpreadsheetId(spreadsheetId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            buildSheetsService().spreadsheets().get(spreadsheetId).execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveContacts(spreadsheetId: String, contacts: List<Contact>) =
        withContext(Dispatchers.IO) {
            val service = buildSheetsService()
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val existingSheets = spreadsheet.sheets.map { it.properties.title }

            val byCity = contacts.groupBy { it.city.trim().ifBlank { "Unknown" } }

            val addSheetRequests = byCity.keys
                .filter { it !in existingSheets }
                .map { city ->
                    Request().setAddSheet(
                        AddSheetRequest().setProperties(SheetProperties().setTitle(city))
                    )
                }

            if (addSheetRequests.isNotEmpty()) {
                service.spreadsheets().batchUpdate(
                    spreadsheetId,
                    BatchUpdateSpreadsheetRequest().setRequests(addSheetRequests)
                ).execute()
            }

            byCity.forEach { (city, cityContacts) ->
                val values = cityContacts.map { contact ->
                    listOf(contact.name, contact.mobile, contact.city) as List<Any>
                }
                service.spreadsheets().values()
                    .append(
                        spreadsheetId,
                        "$city!A1",
                        ValueRange().setValues(values)
                    )
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()
            }
        }
}

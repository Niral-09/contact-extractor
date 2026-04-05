package com.contactextractor.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    fun setGeminiApiKey(key: String) = prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()

    fun getSpreadsheetId(): String = prefs.getString(KEY_SPREADSHEET_ID, "") ?: ""
    fun setSpreadsheetId(id: String) = prefs.edit().putString(KEY_SPREADSHEET_ID, id).apply()

    fun isConfigured(): Boolean =
        getGeminiApiKey().isNotBlank() && getSpreadsheetId().isNotBlank()

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
    }
}

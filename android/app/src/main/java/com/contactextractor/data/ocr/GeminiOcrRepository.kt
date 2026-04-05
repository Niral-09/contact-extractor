package com.contactextractor.data.ocr

import com.contactextractor.data.model.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

private const val GEMINI_PROMPT = """Extract all contacts from this image. Each contact must have:
- name (string, transliterate Gujarati to English if needed)
- mobile (exactly 10 digits, digits only, no country code)
- city (string)
Return ONLY a JSON array: [{"name":"...","mobile":"...","city":"..."}]
If a field is missing or unclear, use empty string. No markdown, no explanation."""

@Singleton
class GeminiOcrRepository @Inject constructor(
    private val api: GeminiApi
) {
    private val gson = Gson()

    suspend fun extractContacts(
        apiKey: String,
        base64Image: String,
        mimeType: String = "image/jpeg",
        onRetryCountdown: (suspend (Int) -> Unit)? = null
    ): List<Contact> {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = GEMINI_PROMPT),
                        GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.1f, maxOutputTokens = 2048)
        )

        repeat(5) { attempt ->
            val response = api.generateContent(apiKey, request)
            when {
                response.isSuccessful -> {
                    val text = response.body()
                        ?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()
                        ?.text ?: return emptyList()
                    return parseContactsJson(text)
                }
                response.code() == 429 -> {
                    if (attempt < 4) {
                        for (seconds in 60 downTo 1) {
                            onRetryCountdown?.invoke(seconds)
                            delay(1_000L)
                        }
                    }
                }
                else -> return emptyList()
            }
        }
        return emptyList()
    }

    fun parseContactsJson(raw: String): List<Contact> {
        return try {
            val cleaned = raw
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
            val type = object : TypeToken<List<ContactJson>>() {}.type
            val items: List<ContactJson> = gson.fromJson(cleaned, type)
            items
                .filter { it.mobile.filter(Char::isDigit).length == 10 }
                .map { item ->
                    Contact(
                        name = item.name.trim(),
                        mobile = item.mobile.filter(Char::isDigit),
                        city = item.city.trim()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

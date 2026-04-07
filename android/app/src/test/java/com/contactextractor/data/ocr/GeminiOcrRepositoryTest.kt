package com.contactextractor.data.ocr

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class GeminiOcrRepositoryTest {

    private val api = mockk<GeminiApi>()
    private val repo = GeminiOcrRepository(api)

    @Test
    fun `parseContactsJson returns list of contacts from valid JSON`() {
        val json = """[{"name":"Amit","mobile":"9876543210","city":"Ahmedabad"}]"""
        val result = repo.parseContactsJson(json)
        assertEquals(1, result.size)
        assertEquals("Amit", result[0].name)
        assertEquals("9876543210", result[0].mobile)
        assertEquals("Ahmedabad", result[0].city)
    }

    @Test
    fun `parseContactsJson strips markdown code block before parsing`() {
        val json = "```json\n[{\"name\":\"Ravi\",\"mobile\":\"9123456789\",\"city\":\"Surat\"}]\n```"
        val result = repo.parseContactsJson(json)
        assertEquals(1, result.size)
        assertEquals("Ravi", result[0].name)
    }

    @Test
    fun `parseContactsJson returns empty list when JSON is malformed`() {
        val result = repo.parseContactsJson("not json at all")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseContactsJson filters contacts with invalid mobile`() {
        val json = """[
            {"name":"Valid","mobile":"9876543210","city":"Patan"},
            {"name":"Bad","mobile":"123","city":"Surat"}
        ]"""
        val result = repo.parseContactsJson(json)
        assertEquals(1, result.size)
        assertEquals("Valid", result[0].name)
    }

    @Test
    fun `extractContacts returns contacts on successful API response`() = runTest {
        val responseJson = """[{"name":"Test","mobile":"9000000001","city":"Rajkot"}]"""
        val geminiResponse = GeminiResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(
                        parts = listOf(GeminiPart(text = responseJson))
                    )
                )
            )
        )
        coEvery { api.generateContent(any(), any()) } returns Response.success(geminiResponse)

        val result = repo.extractContacts("fake-api-key", "base64imagedata")
        assertEquals(1, result.size)
        assertEquals("Test", result[0].name)
    }

    @Test
    fun `extractContacts returns empty list when API response has no candidates`() = runTest {
        val geminiResponse = GeminiResponse(candidates = emptyList())
        coEvery { api.generateContent(any(), any()) } returns Response.success(geminiResponse)

        val result = repo.extractContacts("fake-api-key", "base64imagedata")
        assertTrue(result.isEmpty())
    }
}

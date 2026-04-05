package com.contactextractor.ui.results

import com.contactextractor.data.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VcfBuilderTest {

    @Test
    fun `build produces valid vCard for single contact`() {
        val contacts = listOf(Contact(name = "Amit", mobile = "9876543210", city = "Patan"))
        val vcf = VcfBuilder.build(contacts)
        assertTrue(vcf.contains("BEGIN:VCARD"))
        assertTrue(vcf.contains("VERSION:3.0"))
        assertTrue(vcf.contains("FN:Patan-Amit"))
        assertTrue(vcf.contains("N:Patan-Amit;;;;"))
        assertTrue(vcf.contains("TEL;TYPE=CELL:9876543210"))
        assertTrue(vcf.contains("END:VCARD"))
    }

    @Test
    fun `build produces one vCard block per contact`() {
        val contacts = listOf(
            Contact(name = "A", mobile = "9000000001", city = "Surat"),
            Contact(name = "B", mobile = "9000000002", city = "Rajkot")
        )
        val vcf = VcfBuilder.build(contacts)
        assertEquals(2, vcf.split("BEGIN:VCARD").size - 1)
    }

    @Test
    fun `build returns empty string for empty list`() {
        assertEquals("", VcfBuilder.build(emptyList()))
    }
}

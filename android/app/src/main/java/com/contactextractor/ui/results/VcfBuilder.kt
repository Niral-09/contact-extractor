package com.contactextractor.ui.results

import com.contactextractor.data.model.Contact

object VcfBuilder {
    fun build(contacts: List<Contact>): String {
        if (contacts.isEmpty()) return ""
        return contacts.joinToString(separator = "\n") { contact ->
            val displayName = contact.displayName()
            """BEGIN:VCARD
VERSION:3.0
N:$displayName;;;;
FN:$displayName
TEL;TYPE=CELL:${contact.mobile}
END:VCARD""".trimIndent()
        }
    }
}

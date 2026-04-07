package com.contactextractor.data.model

import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobile: String,
    val city: String
) {
    fun isValid(): Boolean =
        name.isNotBlank() && mobile.length == 10 && mobile.all { it.isDigit() } && city.isNotBlank()

    fun displayName(): String = "${city.trim()}-${name.trim()}"
}

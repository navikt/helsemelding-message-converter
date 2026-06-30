package no.nav.helsemelding.message.model

data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)

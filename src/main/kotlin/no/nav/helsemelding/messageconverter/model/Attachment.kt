package no.nav.helsemelding.messageconverter.model

data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)

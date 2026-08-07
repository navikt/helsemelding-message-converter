package no.nav.helsemelding.messageconverter.model

/**
 * An attachment extracted from a MsgHead XML message.
 *
 * @property description human-readable description of the attachment (required)
 * @property contentType the MIME content type of the attachment, e.g. `"application/pdf"` (required)
 * @property contentBase64 the Base64-encoded content of the attachment (required)
 */
data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)

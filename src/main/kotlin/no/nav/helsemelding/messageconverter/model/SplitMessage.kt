package no.nav.helsemelding.messageconverter.model

data class SplitMessage(
    val messageWithoutAttachmentsXml: String,
    val attachments: List<Attachment>
)

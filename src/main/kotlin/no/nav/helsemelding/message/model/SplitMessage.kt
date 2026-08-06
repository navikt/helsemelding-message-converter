package no.nav.helsemelding.message.model

/**
 * The result of splitting a MsgHead XML message into its main content and attachments.
 *
 * @property messageWithoutAttachmentsXml the MsgHead XML with all attachment documents removed
 * @property attachments the list of attachments extracted from the original XML
 */
data class SplitMessage(
    val messageWithoutAttachmentsXml: String,
    val attachments: List<Attachment>
)

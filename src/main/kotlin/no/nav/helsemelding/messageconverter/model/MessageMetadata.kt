package no.nav.helsemelding.messageconverter.model

/**
 * Metadata extracted from a MsgHead XML message.
 *
 * @property senderHerId the sender's her id, preferring the practitioner over the organization
 * @property receiverHerIds the distinct her ids of primary and additional receivers, preferring practitioners to organizations
 * @property messageTypeIdentificator the message type from MsgInfo/Type/@V, e.g. `"DIALOG_NOTAT"`
 */
data class MessageMetadata(
    val senderHerId: Int,
    val receiverHerIds: List<Int>,
    val messageTypeIdentificator: String
)

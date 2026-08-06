package no.nav.helsemelding.messageconverter.msghead.model

import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.messageconverter.msghead.model.provider.Provider
import java.time.Instant
import kotlin.uuid.Uuid

sealed interface OutgoingMessage {
    val id: String
    val type: OutgoingDialogMessageType
    val message: String?
    val attachment: String?
    val provider: Provider
    val employee: Employee
    val createdAt: Instant
    val docId: Uuid
}

data class InquiryMessage(
    override val id: String,
    val conversationReference: ConversationReference,
    override val type: OutgoingDialogMessageType,
    override val message: String?,
    override val attachment: String?,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: Instant,
    override val docId: Uuid
) : OutgoingMessage

data class MemoMessage(
    override val id: String,
    val conversationReference: ConversationReference,
    override val type: OutgoingDialogMessageType,
    override val message: String?,
    override val attachment: String?,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: Instant,
    override val docId: Uuid
) : OutgoingMessage

data class FollowUpPlanMessage(
    override val id: String,
    override val type: OutgoingDialogMessageType,
    override val message: String,
    override val attachment: String,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: Instant,
    override val docId: Uuid
) : OutgoingMessage

package no.nav.helsemelding.message.msghead.model

import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.client.providerregistry.model.Provider
import java.time.LocalDateTime
import kotlin.uuid.Uuid

sealed interface OutgoingMessage {
    val id: String
    val type: OutgoingDialogMessageType
    val message: String?
    val attachment: String?
    val provider: Provider
    val employee: Employee
    val createdAt: LocalDateTime
    val dokId: Uuid
}

data class InquiryMessage(
    override val id: String,
    val conversationReference: ConversationReference,
    override val type: OutgoingDialogMessageType,
    override val message: String?,
    override val attachment: String?,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: LocalDateTime,
    override val dokId: Uuid
) : OutgoingMessage

data class MemoMessage(
    override val id: String,
    val conversationReference: ConversationReference,
    override val type: OutgoingDialogMessageType,
    override val message: String?,
    override val attachment: String?,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: LocalDateTime,
    override val dokId: Uuid
) : OutgoingMessage

data class FollowUpPlanMessage(
    override val id: String,
    override val type: OutgoingDialogMessageType = OutgoingDialogMessageType.FOLLOW_UP_PLAN,
    override val message: String,
    override val attachment: String,
    override val provider: Provider,
    override val employee: Employee,
    override val createdAt: LocalDateTime,
    override val dokId: Uuid
) : OutgoingMessage

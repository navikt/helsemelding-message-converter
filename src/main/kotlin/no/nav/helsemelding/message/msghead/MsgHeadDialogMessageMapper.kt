package no.nav.helsemelding.message.msghead

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.MappingError
import no.nav.helsemelding.message.msghead.mapper.createFollowUpPlan
import no.nav.helsemelding.message.msghead.mapper.createInquiry
import no.nav.helsemelding.message.msghead.mapper.createMemo
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.OutgoingMessage

private const val INCOMING_DIALOG_MESSAGE_VERSION = 1

class MsgHeadDialogMessageMapper {
    fun toIncomingDialogMessage(msgHead: XMLMsgHead): Either<ConversionError, IncomingDialogMessage> =
        either {
            IncomingDialogMessage(
                INCOMING_DIALOG_MESSAGE_VERSION,
                msgHead.dialogId().bind(),
                IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                msgHead.createdAt().bind(),
                msgHead.patientId().bind(),
                msgHead.sender().bind(),
                msgHead.conversationReference(),
                msgHead.messageText(),
                msgHead.extractAttachmentDocuments().size
            )
        }

    fun toMsgHead(dialogMessage: OutgoingMessage): Either<ConversionError, XMLMsgHead> {
        return when (dialogMessage) {
            is MemoMessage -> createMemo(dialogMessage)
            is InquiryMessage -> createInquiry(dialogMessage)
            is FollowUpPlanMessage -> createFollowUpPlan(dialogMessage)
        }.right()
    }

    private fun XMLMsgHead.dialogId(): Either<ConversionError, String> =
        msgInfo?.msgId.toRequiredField("msgInfo.msgId")

    private fun XMLMsgHead.createdAt(): Either<ConversionError, String> =
        msgInfo?.genDate?.toString().toRequiredField("msgInfo.genDate")

    private fun XMLMsgHead.patientId(): Either<ConversionError, String> =
        msgInfo
            ?.patient
            ?.ident
            ?.firstOrNull()
            ?.id
            .toRequiredField("msgInfo.patient.ident[0].id")

    private fun XMLMsgHead.sender(): Either<ConversionError, Sender> =
        either {
            Sender(
                senderProviderId().bind(),
                senderSigningProviderId().bind()
            )
        }

    private fun XMLMsgHead.senderProviderId(): Either<ConversionError, String> {
        val providerId = msgInfo
            ?.sender
            ?.organisation
            ?.organisation
            ?.ident
            ?.firstOrNull()
            ?.id
            ?: msgInfo
                ?.sender
                ?.organisation
                ?.ident
                ?.firstOrNull()
                ?.id
        return providerId.toRequiredField("msgInfo.sender.organisation.ident[0].id")
    }

    private fun XMLMsgHead.senderSigningProviderId(): Either<ConversionError, String> {
        val signingProviderId = msgInfo
            ?.sender
            ?.organisation
            ?.healthcareProfessional
            ?.ident
            ?.firstOrNull()
            ?.id
        return signingProviderId?.let { Either.Right(it) } ?: senderProviderId()
    }

    private fun XMLMsgHead.conversationReference(): ConversationReference? =
        msgInfo?.conversationRef?.let { conversationRef ->
            ConversationReference(
                conversationRef.refToParent,
                conversationRef.refToConversation
            )
        }

    private fun XMLMsgHead.messageText(): String =
        when (
            val content = document
                .firstOrNull()
                ?.refDoc
                ?.content
                ?.any
                ?.firstOrNull()
        ) {
            is XMLDialogmelding -> content.sporsmal()
            else -> ""
        }

    private fun XMLDialogmelding.sporsmal(): String =
        foresporsel
            .firstOrNull()
            ?.sporsmal
            .orEmpty()

    private fun String?.toRequiredField(field: String): Either<ConversionError, String> =
        this?.let { Either.Right(it) } ?: Either.Left(
            MappingError(
                message = "Missing required MsgHead field: $field",
                field = field
            )
        )
}

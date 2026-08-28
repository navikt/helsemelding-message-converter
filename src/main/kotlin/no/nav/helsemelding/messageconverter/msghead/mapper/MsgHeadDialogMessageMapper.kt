package no.nav.helsemelding.messageconverter.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.IncomingType
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.msghead.MSG_TYPE_DIALOG_NOTE
import no.nav.helsemelding.messageconverter.msghead.MSG_TYPE_DIALOG_RESPONSE
import no.nav.helsemelding.messageconverter.msghead.extractAttachmentDocuments
import no.nav.helsemelding.messageconverter.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.messageconverter.msghead.model.InquiryMessage
import no.nav.helsemelding.messageconverter.msghead.model.MemoMessage
import no.nav.helsemelding.messageconverter.msghead.model.OutgoingMessage
import no.nav.helse.dialogmelding.CV as CodedValue

private const val INCOMING_DIALOG_MESSAGE_VERSION = 1

class MsgHeadDialogMessageMapper {
    fun toIncomingDialogMessage(msgHead: XMLMsgHead): Either<ConversionError, IncomingDialogMessage> =
        either {
            IncomingDialogMessage(
                INCOMING_DIALOG_MESSAGE_VERSION,
                msgHead.dialogId().bind(),
                msgHead.dialogMessageType().bind(),
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
        }
    }

    private fun XMLMsgHead.dialogMessageType(): Either<ConversionError, IncomingDialogMessageType> =
        either {
            val incomingType = msgInfo?.type?.v
                .toRequiredField("msgInfo.type.v").bind()
                .toIncomingType().bind()

            val messageTopic = dialogMessage()
                ?.topic()

            messageTopic
                ?.toIncomingDialogMessageType(incomingType)
                ?: raise(unknownDialogMessageType(messageTopic))
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
            is XMLDialogmelding -> content.messageText()
            else -> ""
        }

    private fun XMLDialogmelding.messageText(): String =
        notat.firstOrNull()?.tekstNotatInnhold
            ?: foresporsel.firstOrNull()?.sporsmal
            ?: ""

    private fun String?.toRequiredField(field: String): Either<ConversionError, String> =
        this?.let { Either.Right(it) } ?: Either.Left(
            MappingError(
                message = "Missing required MsgHead field: $field",
                field = field
            )
        )

    private fun XMLMsgHead.dialogMessage(): XMLDialogmelding? =
        document
            .firstOrNull()
            ?.refDoc
            ?.content
            ?.any
            ?.firstOrNull() as? XMLDialogmelding

    private fun XMLDialogmelding.topic(): CodedValue? =
        notat.firstOrNull()?.temaKodet ?: foresporsel.firstOrNull()?.typeForesp

    private fun CodedValue.codeSystem(): Int? = s?.takeLast(4)?.toIntOrNull()

    private fun CodedValue.code(): Int? = v?.toIntOrNull()

    private fun CodedValue.toIncomingDialogMessageType(incomingType: IncomingType): IncomingDialogMessageType? =
        IncomingDialogMessageType.entries
            .firstOrNull { it.codeSystem == codeSystem() && it.code == code() && it.messageType == incomingType }

    private fun String.toIncomingType(): Either<ConversionError, IncomingType> = when (this) {
        MSG_TYPE_DIALOG_NOTE -> Either.Right(IncomingType.DIALOG_NOTE)
        MSG_TYPE_DIALOG_RESPONSE -> Either.Right(IncomingType.DIALOG_RESPONSE)
        else -> Either.Left(MappingError(message = "Unknown message type: $this", field = "msgInfo.type.v"))
    }

    private fun unknownDialogMessageType(messageTopic: CodedValue?): MappingError {
        val codeSystem = messageTopic?.codeSystem()
        val code = messageTopic?.code()

        return MappingError(
            message = "Unknown dialog message type: codeSystem=$codeSystem, code=$code",
            field = "document[0].refDoc.content.Dialogmelding.temaKodet"
        )
    }
}

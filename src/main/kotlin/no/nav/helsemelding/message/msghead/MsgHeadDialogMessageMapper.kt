package no.nav.helsemelding.message.msghead

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.msgHead.XMLConversationRef
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.MappingError

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

    fun toMsgHead(dialogMessage: OutgoingDialogMessage): Either<ConversionError, XMLMsgHead> =
        Either.Right(
            XMLMsgHead().apply {
                msgInfo = XMLMsgInfo()
                setDialogId(dialogMessage.id)
                setPatientId(dialogMessage.patientIdent)
                setSenderProviderId(dialogMessage.providerId)
                setConversationReference(dialogMessage.conversationReference)
                setMessageText(dialogMessage.message.orEmpty())
            }
        )

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
        document
            .firstOrNull()
            ?.refDoc
            ?.content
            ?.any
            ?.firstOrNull()
            ?.toString()
            .orEmpty()

    private fun XMLMsgHead.setDialogId(dialogId: String) {
        msgInfo = msgInfo ?: XMLMsgInfo()
        msgInfo.msgId = dialogId
    }

    private fun XMLMsgHead.setPatientId(patientId: String) {
        msgInfo = msgInfo ?: XMLMsgInfo()
        msgInfo.patient = XMLPatient().apply {
            ident.add(XMLIdent().apply { id = patientId })
        }
    }

    private fun XMLMsgHead.setSenderProviderId(providerId: String) {
        msgInfo = msgInfo ?: XMLMsgInfo()
        msgInfo.sender = XMLSender().apply {
            organisation = XMLOrganisation().apply {
                ident.add(XMLIdent().apply { id = providerId })
            }
        }
    }

    private fun XMLMsgHead.setConversationReference(conversationReference: ConversationReference?) {
        msgInfo = msgInfo ?: XMLMsgInfo()
        msgInfo.conversationRef = conversationReference?.let {
            XMLConversationRef().apply {
                refToParent = it.parentMessageId
                refToConversation = it.conversationId
            }
        }
    }

    private fun XMLMsgHead.setMessageText(messageText: String) {
        document.clear()
        document.add(
            XMLDocument().apply {
                refDoc = XMLRefDoc().apply {
                    content = XMLRefDoc.Content().apply {
                        any.add(messageText)
                    }
                }
            }
        )
    }

    private fun String?.toRequiredField(field: String): Either<ConversionError, String> =
        this?.let { Either.Right(it) } ?: Either.Left(
            MappingError(
                message = "Missing required MsgHead field: $field",
                field = field
            )
        )
}

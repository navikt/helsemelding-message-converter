package no.nav.helsemelding.messageconverter.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.IncomingType
import no.nav.helsemelding.jsonschema.core.model.Provider
import no.nav.helsemelding.jsonschema.core.model.ProviderOffice
import no.nav.helsemelding.jsonschema.core.model.Signature
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.msghead.MSG_TYPE_DIALOG_NOTE
import no.nav.helsemelding.messageconverter.msghead.MSG_TYPE_DIALOG_RESPONSE
import no.nav.helsemelding.messageconverter.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.messageconverter.msghead.model.InquiryMessage
import no.nav.helsemelding.messageconverter.msghead.model.MemoMessage
import no.nav.helsemelding.messageconverter.msghead.model.OutgoingMessage
import java.time.Clock
import java.time.Instant
import no.nav.helse.dialogmelding.CV as CodedValue

private const val INCOMING_DIALOG_MESSAGE_VERSION = 1

class DialogMessageMapper(private val clock: Clock = Clock.systemUTC()) {
    fun toIncomingDialogMessage(msgHead: XMLMsgHead): Either<ConversionError, IncomingDialogMessage> =
        either {
            IncomingDialogMessage(
                version = INCOMING_DIALOG_MESSAGE_VERSION,
                id = msgHead.dialogId().bind(),
                type = msgHead.dialogMessageType().bind(),
                receivedAt = Instant.now(clock).toString(),
                patientIdent = msgHead.patientId().bind(),
                conversationReference = msgHead.conversationReference(),
                message = msgHead.messageText(),
                numberOfAttachments = 0,
                provider = msgHead.provider().bind(),
                signature = msgHead.signature().bind(),
                documentId = msgHead.documentId().bind()
            )
        }

    private fun XMLMsgHead.conversationReference(): ConversationReference? =
        msgInfo?.conversationRef?.let { conversationRef ->
            ConversationReference(
                conversationRef.refToParent,
                conversationRef.refToConversation
            )
        }

    private fun XMLMsgHead.provider(): Either<ConversionError, Provider> =
        either {
            Provider(
                ident = providerId().bind(),
                hprNumber = providerHprNumber(),
                office = providerOffice().bind()
            )
        }

    private fun XMLMsgHead.providerOffice(): Either<ConversionError, ProviderOffice> =
        either {
            ProviderOffice(
                orgNumber = providerOfficeOrgNumber(),
                orgName = providerOfficeName().bind(),
                herId = providerOfficeHerId()
            )
        }

    private fun XMLMsgHead.signature(): Either<ConversionError, Signature> =
        either {
            // TODO: Temporary solution. The values should be extracted from the signature itself.
            Signature(
                signingProviderIdent = providerId().bind(),
                signedAt = createdAt().bind()
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
            ?.identifier("FNR", "DNR")
            .toRequiredField("msgInfo.patient.ident[0].id")

    private fun XMLMsgHead.providerId(): Either<ConversionError, String> =
        providerOfficeOrganisation()
            ?.healthcareProfessional
            ?.ident
            ?.identifier("FNR", "DNR")
            .toRequiredField("msgInfo.sender.organisation.healthcareProfessional.ident[FNR/DNR].id")

    private fun XMLMsgHead.providerHprNumber(): String? =
        providerOfficeOrganisation()
            ?.healthcareProfessional
            ?.ident
            ?.identifier("HPR")

    private fun XMLMsgHead.providerOfficeOrgNumber(): String? =
        providerOfficeOrganisation()
            ?.ident
            ?.identifier("ENH")

    private fun XMLMsgHead.providerOfficeName(): Either<ConversionError, String> =
        providerOfficeOrganisation()
            ?.organisationName
            .toRequiredField("msgInfo.sender.organisation.organisationName")

    private fun XMLMsgHead.providerOfficeHerId(): String? =
        providerOfficeOrganisation()
            ?.ident
            ?.identifier("HER")

    private fun XMLMsgHead.providerOfficeOrganisation(): XMLOrganisation? =
        msgInfo?.sender?.organisation

    private fun List<XMLIdent>.identifier(vararg types: String): String? =
        firstOrNull { it.typeId?.v in types }?.id

    private fun XMLMsgHead.documentId(): Either<ConversionError, String> =
        dialogMessage()
            ?.notat
            ?.firstOrNull()
            ?.dokIdNotat
            .toRequiredField("document[0].refDoc.content.Dialogmelding.notat[0].dokIdNotat")

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
        notat.firstOrNull()?.tekstNotatInnhold ?: ""

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

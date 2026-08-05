package no.nav.helsemelding.message.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.FOLLOW_UP_PLAN
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEDICAL_CERTIFICATE_RETURN
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_CANCELLATION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_EXEMPTION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_INVITATION_2
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_INVITATION_3
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_RESCHEDULE_2
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_RESCHEDULE_3
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_FEEDBACK
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_INFORMATION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_MESSAGE
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.PATIENT_REQUEST
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.PATIENT_REQUEST_REMINDER
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.RETURN_TO_WORK_NOTIFICATION
import no.nav.helsemelding.message.error.AttachmentMissingError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.OutgoingMessage

internal fun createOutgoingMessage(
    dialogMessage: OutgoingDialogMessage,
    additionalMessageInfo: AdditionalMessageInfo
): Either<ConversionError, OutgoingMessage> = either {
    when (dialogMessage.type) {
        MEETING_INVITATION_2,
        MEETING_RESCHEDULE_2,
        MEETING_INVITATION_3,
        MEETING_RESCHEDULE_3,
        PATIENT_REQUEST,
        PATIENT_REQUEST_REMINDER -> createInquiryMessage(dialogMessage, additionalMessageInfo)

        FOLLOW_UP_PLAN -> createFollowUpPlanMessage(dialogMessage, additionalMessageInfo).bind()

        RETURN_TO_WORK_NOTIFICATION,
        MEDICAL_CERTIFICATE_RETURN,
        MEETING_CANCELLATION,
        MEETING_EXEMPTION,
        NAV_FEEDBACK,
        NAV_MESSAGE,
        NAV_INFORMATION -> createMemoMessage(dialogMessage, additionalMessageInfo)
    }
}

private fun createFollowUpPlanMessage(
    dialogMessage: OutgoingDialogMessage,
    additionalMessageInfo: AdditionalMessageInfo
): Either<ConversionError, FollowUpPlanMessage> = either {
    val attachment = ensureNotNull(dialogMessage.attachment?.takeIf { it.isNotEmpty() }) {
        AttachmentMissingError(
            message = "Missing follow-up plan attachment"
        )
    }

    FollowUpPlanMessage(
        id = dialogMessage.id,
        type = dialogMessage.type,
        message = "Åpne PDF-vedlegg",
        attachment = attachment,
        provider = additionalMessageInfo.provider,
        employee = additionalMessageInfo.employee,
        createdAt = additionalMessageInfo.createdAt,
        docId = additionalMessageInfo.docId
    )
}

private fun createInquiryMessage(
    dialogMessage: OutgoingDialogMessage,
    additionalMessageInfo: AdditionalMessageInfo
): InquiryMessage = InquiryMessage(
    id = dialogMessage.id,
    conversationReference = dialogMessage.conversationReference ?: ConversationReference(
        parentMessageId = dialogMessage.id,
        conversationId = dialogMessage.id
    ),
    type = dialogMessage.type,
    message = dialogMessage.message,
    attachment = dialogMessage.attachment,
    provider = additionalMessageInfo.provider,
    employee = additionalMessageInfo.employee,
    createdAt = additionalMessageInfo.createdAt,
    docId = additionalMessageInfo.docId
)

private fun createMemoMessage(
    dialogMessage: OutgoingDialogMessage,
    additionalMessageInfo: AdditionalMessageInfo
): MemoMessage = MemoMessage(
    id = dialogMessage.id,
    conversationReference = dialogMessage.conversationReference ?: ConversationReference(
        parentMessageId = dialogMessage.id,
        conversationId = dialogMessage.id
    ),
    type = dialogMessage.type,
    message = dialogMessage.message,
    attachment = dialogMessage.attachment,
    provider = additionalMessageInfo.provider,
    employee = additionalMessageInfo.employee,
    createdAt = additionalMessageInfo.createdAt,
    docId = additionalMessageInfo.docId
)

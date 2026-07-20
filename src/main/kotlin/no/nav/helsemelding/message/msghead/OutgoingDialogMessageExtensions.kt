package no.nav.helsemelding.message.msghead

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.error.AttachmentMissingError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.OutgoingMessage

fun OutgoingDialogMessage.toOutgoingMessage(
    additionalMessageInfo: AdditionalMessageInfo
): Either<ConversionError, OutgoingMessage> {
    return when (type) {
        OutgoingDialogMessageType.MEETING_INVITATION_2,
        OutgoingDialogMessageType.MEETING_RESCHEDULE_2,
        OutgoingDialogMessageType.MEETING_INVITATION_3,
        OutgoingDialogMessageType.MEETING_RESCHEDULE_3,
        OutgoingDialogMessageType.PATIENT_REQUEST,
        OutgoingDialogMessageType.PATIENT_REQUEST_REMINDER -> toInquiryMessage(additionalMessageInfo).right()

        OutgoingDialogMessageType.FOLLOW_UP_PLAN -> toFollowUpPlanMessage(additionalMessageInfo)

        OutgoingDialogMessageType.RETURN_TO_WORK_NOTIFICATION,
        OutgoingDialogMessageType.MEDICAL_CERTIFICATE_RETURN,
        OutgoingDialogMessageType.MEETING_CANCELLATION,
        OutgoingDialogMessageType.MEETING_EXEMPTION,
        OutgoingDialogMessageType.NAV_FEEDBACK,
        OutgoingDialogMessageType.NAV_MESSAGE,
        OutgoingDialogMessageType.NAV_INFORMATION -> toMemoMessage(additionalMessageInfo).right()
    }
}

private fun OutgoingDialogMessage.toFollowUpPlanMessage(
    additionalMessageInfo: AdditionalMessageInfo
): Either<ConversionError, FollowUpPlanMessage> {
    val attachment = attachment
    if (attachment.isNullOrEmpty()) {
        return AttachmentMissingError(
            message = "Failed to convert JSON with OutgoingDialogMessageType: FOLLOW_UP_PLAN to FollowUpPlanMessage"
        ).left()
    }

    return FollowUpPlanMessage(
        id = id,
        message = "Åpne PDF-vedlegg",
        attachment = attachment,
        provider = additionalMessageInfo.provider,
        arbeidstaker = additionalMessageInfo.arbeidstaker,
        createdAt = additionalMessageInfo.createdAt,
        dokId = additionalMessageInfo.dokId
    ).right()
}

private fun OutgoingDialogMessage.toInquiryMessage(additionalMessageInfo: AdditionalMessageInfo): InquiryMessage =
    InquiryMessage(
        id = id,
        conversationReference = conversationReference ?: ConversationReference(
            parentMessageId = id,
            conversationId = id
        ),
        type = type,
        message = message,
        attachment = attachment,
        provider = additionalMessageInfo.provider,
        arbeidstaker = additionalMessageInfo.arbeidstaker,
        createdAt = additionalMessageInfo.createdAt,
        dokId = additionalMessageInfo.dokId
    )

private fun OutgoingDialogMessage.toMemoMessage(additionalMessageInfo: AdditionalMessageInfo): MemoMessage =
    MemoMessage(
        id = id,
        conversationReference = conversationReference ?: ConversationReference(
            parentMessageId = id,
            conversationId = id
        ),
        type = type,
        message = message,
        attachment = attachment,
        provider = additionalMessageInfo.provider,
        arbeidstaker = additionalMessageInfo.arbeidstaker,
        createdAt = additionalMessageInfo.createdAt,
        dokId = additionalMessageInfo.dokId
    )

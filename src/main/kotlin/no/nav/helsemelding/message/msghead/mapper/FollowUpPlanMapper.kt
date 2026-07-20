package no.nav.helsemelding.message.msghead.mapper

import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage

fun createFollowUpPlan(message: FollowUpPlanMessage): XMLMsgHead {
    return XMLMsgHead().apply {
        msgInfo = createBaseDialogMessage(message).apply {
            receiver = createReceiver(message.provider) { roleToPatient() }
        }
        document.add(
            createDialogMessageDocument(
                outgoingMessage = message,
                dialogmelding = followUpPlan(message)
            )
        )
        document.add(
            createAttachmentDocument(
                message.attachment.toByteArray(),
                message.createdAt
            )
        )
    }
}

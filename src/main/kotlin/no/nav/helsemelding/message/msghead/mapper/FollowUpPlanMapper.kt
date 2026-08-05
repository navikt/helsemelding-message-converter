package no.nav.helsemelding.message.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage

internal fun createFollowUpPlan(message: FollowUpPlanMessage): Either<ConversionError, XMLMsgHead> =
    either {
        XMLMsgHead().apply {
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
                    message.attachment,
                    message.createdAt
                ).bind()
            )
        }
    }

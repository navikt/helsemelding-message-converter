package no.nav.helsemelding.messageconverter.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.msghead.model.MemoMessage

internal fun createMemo(message: MemoMessage): Either<ConversionError, XMLMsgHead> =
    either {
        XMLMsgHead().apply {
            msgInfo = createBaseDialogMessage(message).apply {
                receiver = createReceiver(message.provider)
                conversationRef = createConversationRef(message.conversationReference)
            }
            document.add(
                createDialogMessageDocument(
                    outgoingMessage = message,
                    dialogMessage = createMemoDialogMessage(message)
                )
            )
            message.attachment?.let {
                document.add(createAttachmentDocument(it, message.createdAt).bind())
            }
        }
    }

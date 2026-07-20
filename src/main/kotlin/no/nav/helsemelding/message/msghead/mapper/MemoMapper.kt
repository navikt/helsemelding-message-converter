package no.nav.helsemelding.message.msghead.mapper

import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.message.msghead.model.MemoMessage

fun createMemo(message: MemoMessage): XMLMsgHead {
    return XMLMsgHead().apply {
        msgInfo = createBaseDialogMessage(message).apply {
            receiver = createReceiver(message.provider)
            conversationRef = createConversationRef(message.conversationReference)
        }
        document.add(
            createDialogMessageDocument(
                outgoingMessage = message,
                dialogmelding = memo(message)
            )
        )
        message.attachment?.let {
            document.add(createAttachmentDocument(it.toByteArray(), message.createdAt))
        }
    }
}

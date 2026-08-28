package no.nav.helsemelding.messageconverter.msghead

import arrow.core.Either
import no.nav.helse.base64container.Base64Container
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.messageconverter.error.AttachmentError
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.model.Attachment
import java.util.Base64

const val ATTACHMENT_TYPE = "A"
const val MSG_TYPE_DIALOG_NOTE = "DIALOG_NOTAT"
const val MSG_TYPE_DIALOG_RESPONSE = "DIALOG_SVAR"
const val MSG_TYPE_DIALOG_REQUEST = "DIALOG_FORESPORSEL"

private val acceptedMimeTypes = listOf(
    "application/pdf",
    "image/tiff",
    "image/png",
    "image/jpeg",
    "image/pjpeg",
    "image/jpg",
    "image/pjpg"
)

fun XMLMsgHead.extractAttachmentDocuments(): List<XMLDocument> {
    val attachments = mutableListOf<XMLDocument>()

    attachments.addAll(extractAllAttachmentsFromMsgHead())

    document.forEach { document ->
        document.refDoc.content.any.forEach {
            if (it is XMLMsgHead) {
                attachments.addAll(it.extractAttachmentDocuments())
            }
        }
    }

    return attachments
}

fun XMLMsgHead.extractAllAttachmentsFromMsgHead(): List<XMLDocument> =
    this.document.filter { it.isAttachment() }

fun XMLMsgHead.removeAttachmentDocuments() {
    document.removeAll { xmlDocument ->
        xmlDocument.isAttachment()
    }

    document.forEach { xmlDocument ->
        xmlDocument.refDoc.content.any.forEach { content ->
            if (content is XMLMsgHead) {
                content.removeAttachmentDocuments()
            }
        }
    }
}

fun XMLDocument.isAttachment(): Boolean =
    refDoc.msgType.v == ATTACHMENT_TYPE && acceptedMimeTypes.contains(refDoc.mimeType)

fun XMLDocument.toAttachment(): Either<ConversionError, Attachment> =
    Either.catch {
        Attachment(
            description = refDoc.description ?: "",
            contentType = refDoc.mimeType,
            contentBase64 = Base64.getEncoder().encodeToString(toBase64Container().value)
        )
    }.mapLeft { AttachmentError("Could not map MsgHead document to attachment", it) }

fun XMLDocument.toBase64Container(): Base64Container =
    refDoc
        .content
        .any
        .firstOrNull() as? Base64Container
        ?: throw IllegalArgumentException("Attachment document content is not a Base64Container")

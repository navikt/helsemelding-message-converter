package no.nav.helsemelding.message.converter

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helsemelding.message.attachment.AttachmentHandler
import no.nav.helsemelding.message.error.AttachmentError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.json.IncomingDialogMessageSerializer
import no.nav.helsemelding.message.json.OutgoingDialogMessageSerializer
import no.nav.helsemelding.message.model.Attachment
import no.nav.helsemelding.message.msghead.MsgHeadDialogMessageMapper
import no.nav.helsemelding.message.msghead.XmlSerializer
import no.nav.helsemelding.message.msghead.extractAttachmentDocuments
import no.nav.helsemelding.message.msghead.removeAttachmentDocuments
import no.nav.helsemelding.message.msghead.toAttachment

class MsgHeadMessageConverter(
    private val xmlSerializer: XmlSerializer = XmlSerializer(),
    private val incomingDialogMessageSerializer: IncomingDialogMessageSerializer = IncomingDialogMessageSerializer(),
    private val outgoingDialogMessageSerializer: OutgoingDialogMessageSerializer = OutgoingDialogMessageSerializer(),
    private val mapper: MsgHeadDialogMessageMapper = MsgHeadDialogMessageMapper()
) : MessageConverter, AttachmentHandler {
    override fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(xml).bind()
            val dialogMessage = mapper.toIncomingDialogMessage(msgHead).bind()

            incomingDialogMessageSerializer.serialize(dialogMessage).bind()
        }

    override fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String> =
        either {
            val dialogMessage = outgoingDialogMessageSerializer.deserialize(json).bind()
            val msgHead = mapper.toMsgHead(dialogMessage).bind()

            xmlSerializer.serialize(msgHead).bind()
        }

    override fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>> =
        either {
            val msgHead = xmlSerializer.deserialize(msgHeadXml).bind()
            val documents = Either.catch { msgHead.extractAttachmentDocuments() }
                .mapLeft { AttachmentError("Could not extract attachment documents from MsgHead", it) }
                .bind()
            val attachments = mutableListOf<Attachment>()

            for (document in documents) {
                attachments += document.toAttachment().bind()
            }

            attachments
        }

    override fun removeAttachments(msgHeadXml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(msgHeadXml).bind()

            Either.catch { msgHead.removeAttachmentDocuments() }
                .mapLeft { AttachmentError("Could not remove attachment documents from MsgHead", it) }
                .bind()

            xmlSerializer.serialize(msgHead).bind()
        }
}

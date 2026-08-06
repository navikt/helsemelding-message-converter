package no.nav.helsemelding.messageconverter

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helsemelding.messageconverter.attachment.AttachmentHandler
import no.nav.helsemelding.messageconverter.error.AttachmentError
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.json.IncomingDialogMessageSerializer
import no.nav.helsemelding.messageconverter.json.OutgoingDialogMessageSerializer
import no.nav.helsemelding.messageconverter.model.Attachment
import no.nav.helsemelding.messageconverter.model.SplitMessage
import no.nav.helsemelding.messageconverter.msghead.XmlSerializer
import no.nav.helsemelding.messageconverter.msghead.extractAttachmentDocuments
import no.nav.helsemelding.messageconverter.msghead.mapper.MsgHeadDialogMessageMapper
import no.nav.helsemelding.messageconverter.msghead.mapper.createOutgoingMessage
import no.nav.helsemelding.messageconverter.msghead.removeAttachmentDocuments
import no.nav.helsemelding.messageconverter.msghead.toAttachment

class MsgHeadMessageConverter(
    private val xmlSerializer: XmlSerializer = XmlSerializer(),
    private val incomingDialogMessageSerializer: IncomingDialogMessageSerializer = IncomingDialogMessageSerializer(),
    private val outgoingDialogMessageSerializer: OutgoingDialogMessageSerializer = OutgoingDialogMessageSerializer(),
    private val mapper: MsgHeadDialogMessageMapper = MsgHeadDialogMessageMapper(),
    private val additionalMessageInfoProvider: AdditionalMessageInfoProvider = MissingAdditionalMessageInfoProvider()
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
            val additionalMessageInfo = additionalMessageInfoProvider.getAdditionalMessageInfo(dialogMessage).bind()
            val outgoingMessage = createOutgoingMessage(dialogMessage, additionalMessageInfo).bind()
            val msgHead = mapper.toMsgHead(outgoingMessage).bind()

            xmlSerializer.serialize(msgHead).bind()
        }

    override fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage> =
        either {
            SplitMessage(
                messageWithoutAttachmentsXml = removeAttachments(msgHeadXml).bind(),
                attachments = extractAttachments(msgHeadXml).bind()
            )
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

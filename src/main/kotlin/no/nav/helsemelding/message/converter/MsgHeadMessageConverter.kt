package no.nav.helsemelding.message.converter

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helsemelding.message.attachment.AttachmentHandler
import no.nav.helsemelding.message.error.AttachmentError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.json.IncomingDialogMessageSerializer
import no.nav.helsemelding.message.json.OutgoingDialogMessageSerializer
import no.nav.helsemelding.message.model.Attachment
import no.nav.helsemelding.message.model.SplitMessage
import no.nav.helsemelding.message.msghead.XmlSerializer
import no.nav.helsemelding.message.msghead.extractAttachmentDocuments
import no.nav.helsemelding.message.msghead.mapper.MsgHeadDialogMessageMapper
import no.nav.helsemelding.message.msghead.mapper.createOutgoingMessage
import no.nav.helsemelding.message.msghead.removeAttachmentDocuments
import no.nav.helsemelding.message.msghead.toAttachment

/**
 * MsgHead-based implementation of [MessageConverter] and [AttachmentHandler].
 *
 * Handles conversion between MsgHead XML and dialog message JSON, as well as
 * extraction and removal of attachments from MsgHead XML messages.
 *
 * For outgoing conversion, an [AdditionalMessageInfoProvider] must be supplied —
 * the default [MissingAdditionalMessageInfoProvider] will always return an error.
 *
 * @param xmlSerializer serializer for MsgHead XML; defaults to [XmlSerializer]
 * @param incomingDialogMessageSerializer serializer for incoming dialog message JSON
 * @param outgoingDialogMessageSerializer serializer for outgoing dialog message JSON
 * @param mapper mapper between MsgHead and dialog message models
 * @param additionalMessageInfoProvider provider for additional metadata required for outgoing conversion
 */
class MsgHeadMessageConverter(
    private val xmlSerializer: XmlSerializer = XmlSerializer(),
    private val incomingDialogMessageSerializer: IncomingDialogMessageSerializer = IncomingDialogMessageSerializer(),
    private val outgoingDialogMessageSerializer: OutgoingDialogMessageSerializer = OutgoingDialogMessageSerializer(),
    private val mapper: MsgHeadDialogMessageMapper = MsgHeadDialogMessageMapper(),
    private val additionalMessageInfoProvider: AdditionalMessageInfoProvider = MissingAdditionalMessageInfoProvider()
) : MessageConverter, AttachmentHandler {
    /**
     * Converts an incoming dialog message from MsgHead XML to JSON.
     *
     * @param xml the raw MsgHead XML string of the incoming dialog message
     * @return the JSON string representation of the dialog message, or a [ConversionError] on failure
     */
    override fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(xml).bind()
            val dialogMessage = mapper.toIncomingDialogMessage(msgHead).bind()

            incomingDialogMessageSerializer.serialize(dialogMessage).bind()
        }

    /**
     * Converts an outgoing dialog message from JSON to MsgHead XML.
     *
     * Requires an [AdditionalMessageInfoProvider] to be configured in the converter,
     * as additional metadata is needed to construct the MsgHead envelope.
     *
     * @param json the JSON string representation of the outgoing dialog message
     * @return the raw MsgHead XML string, or a [ConversionError] on failure
     */
    override fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String> =
        either {
            val dialogMessage = outgoingDialogMessageSerializer.deserialize(json).bind()
            val additionalMessageInfo = additionalMessageInfoProvider.getAdditionalMessageInfo(dialogMessage).bind()
            val outgoingMessage = createOutgoingMessage(dialogMessage, additionalMessageInfo).bind()
            val msgHead = mapper.toMsgHead(outgoingMessage).bind()

            xmlSerializer.serialize(msgHead).bind()
        }

    /**
     * Splits a MsgHead XML message into its main message and attachments.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return a [SplitMessage] containing the XML without attachments and the extracted attachment list,
     *   or a [ConversionError] on failure
     */
    override fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage> =
        either {
            SplitMessage(
                messageWithoutAttachmentsXml = removeAttachments(msgHeadXml).bind(),
                attachments = extractAttachments(msgHeadXml).bind()
            )
        }

    /**
     * Extracts all attachment documents from a MsgHead XML message.
     *
     * The original XML is not modified.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return a list of [Attachment] objects extracted from the XML, or a [ConversionError] on failure
     */
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

    /**
     * Returns the MsgHead XML with all attachment documents removed.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return the XML string with attachment documents stripped out, or a [ConversionError] on failure
     */
    override fun removeAttachments(msgHeadXml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(msgHeadXml).bind()

            Either.catch { msgHead.removeAttachmentDocuments() }
                .mapLeft { AttachmentError("Could not remove attachment documents from MsgHead", it) }
                .bind()

            xmlSerializer.serialize(msgHead).bind()
        }
}

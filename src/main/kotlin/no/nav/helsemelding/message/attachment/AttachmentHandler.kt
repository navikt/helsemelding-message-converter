package no.nav.helsemelding.message.attachment

import arrow.core.Either
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.model.Attachment
import no.nav.helsemelding.message.model.SplitMessage

/**
 * Handles extraction and removal of attachments from MsgHead XML messages.
 *
 * All operations return [Either] where [Either.Left] contains a [ConversionError]
 * on failure, and [Either.Right] contains the successful result.
 */
interface AttachmentHandler {
    /**
     * Splits a MsgHead XML message into its main message and attachments.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return a [SplitMessage] containing the XML without attachments and the extracted attachment list,
     *   or a [ConversionError] on failure
     */
    fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage>

    /**
     * Extracts all attachment documents from a MsgHead XML message.
     *
     * The original XML is not modified.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return a list of [Attachment] objects extracted from the XML, or a [ConversionError] on failure
     */
    fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>>

    /**
     * Returns the MsgHead XML with all attachment documents removed.
     *
     * @param msgHeadXml the raw MsgHead XML string
     * @return the XML string with attachment documents stripped out, or a [ConversionError] on failure
     */
    fun removeAttachments(msgHeadXml: String): Either<ConversionError, String>
}

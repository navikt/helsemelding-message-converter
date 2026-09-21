package no.nav.helsemelding.messageconverter

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.model.MessageMetadata
import kotlin.uuid.Uuid

/**
 * Handles extraction of metadata from MsgHead XML messages.
 *
 * All operations return [Either] where [Either.Left] contains a [ConversionError]
 * on failure, and [Either.Right] contains the successful result.
 */
interface MetadataExtractor {
    /**
     * Extracts sender and receiver her ids and the message type from a MsgHead XML message.
     *
     * Only the outermost MsgInfo is used. The original XML is not modified.
     *
     * @param xml the raw MsgHead XML string
     * @return a [MessageMetadata] containing sender and receiver her ids and the message type,
     *   or a [ConversionError] on failure
     */
    fun extractMetadata(xml: String): Either<ConversionError, MessageMetadata>

    /**
     * Extracts the message id from the MsgInfo/MsgId element of a MsgHead XML message.
     *
     * @param xml the raw MsgHead XML string
     * @return the message id as a [Uuid], or a [ConversionError] if it is missing or not a valid UUID
     */
    fun extractMessageId(xml: String): Either<ConversionError, Uuid>
}

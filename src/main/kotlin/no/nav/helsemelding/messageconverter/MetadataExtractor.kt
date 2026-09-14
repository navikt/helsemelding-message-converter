package no.nav.helsemelding.messageconverter

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.model.MessageMetadata

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
}

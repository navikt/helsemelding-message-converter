package no.nav.helsemelding.message.converter

import arrow.core.Either
import no.nav.helsemelding.message.error.ConversionError

/**
 * Converts dialog messages between MsgHead XML and JSON formats.
 *
 * All operations return [Either] where [Either.Left] contains a [ConversionError]
 * on failure, and [Either.Right] contains the successful result.
 **/
interface MessageConverter {
    /**
     * Converts an incoming dialog message from MsgHead XML to JSON.
     *
     * @param xml the raw MsgHead XML string of the incoming dialog message
     * @return the JSON string representation of the dialog message, or a [ConversionError] on failure
     */
    fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String>

    /**
     * Converts an outgoing dialog message from JSON to MsgHead XML.
     *
     * Requires an [AdditionalMessageInfoProvider] to be configured in the converter,
     * as additional metadata is needed to construct the MsgHead envelope.
     *
     * @param json the JSON string representation of the outgoing dialog message
     * @return the raw MsgHead XML string, or a [ConversionError] on failure
     */
    fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String>
}

package no.nav.helsemelding.message.converter

import arrow.core.Either
import no.nav.helsemelding.message.error.ConversionError

interface MessageConverter {
    fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String>
    fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String>
}

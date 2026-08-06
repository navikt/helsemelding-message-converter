package no.nav.helsemelding.messageconverter

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError

interface MessageConverter {
    fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String>
    fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String>
}

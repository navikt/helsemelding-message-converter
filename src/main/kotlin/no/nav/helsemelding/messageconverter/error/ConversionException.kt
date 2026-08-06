package no.nav.helsemelding.messageconverter.error

class ConversionException(
    val error: ConversionError
) : RuntimeException(error.message, error.cause)

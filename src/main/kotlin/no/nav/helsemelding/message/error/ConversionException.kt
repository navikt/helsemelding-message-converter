package no.nav.helsemelding.message.error

class ConversionException(
    val error: ConversionError
) : RuntimeException(error.message, error.cause)

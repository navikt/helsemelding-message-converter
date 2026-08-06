package no.nav.helsemelding.messageconverter.util

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.ConversionException

fun <T> Either<ConversionError, T>.toResult(): Result<T> =
    fold(
        { Result.failure(ConversionException(it)) },
        { Result.success(it) }
    )

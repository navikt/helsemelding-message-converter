package no.nav.helsemelding.message.util

import arrow.core.Either
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.ConversionException

fun <T> Either<ConversionError, T>.toResult(): Result<T> =
    fold(
        { Result.failure(ConversionException(it)) },
        { Result.success(it) }
    )

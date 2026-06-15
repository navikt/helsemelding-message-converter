package no.nav.helsemelding.message.json

import arrow.core.Either
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.InvalidJson
import no.nav.helsemelding.message.error.SerializationError

class IncomingDialogMessageSerializer(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun serialize(dialogMessage: IncomingDialogMessage): Either<ConversionError, String> =
        Either.catch { json.encodeToString(dialogMessage) }
            .mapLeft { SerializationError("Could not serialize IncomingDialogMessage to JSON", it) }

    fun deserialize(value: String): Either<ConversionError, IncomingDialogMessage> =
        Either.catch { json.decodeFromString<IncomingDialogMessage>(value) }
            .mapLeft { InvalidJson("Could not deserialize IncomingDialogMessage JSON", it) }
}

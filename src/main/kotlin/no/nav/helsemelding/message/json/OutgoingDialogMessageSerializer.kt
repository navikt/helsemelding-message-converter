package no.nav.helsemelding.message.json

import arrow.core.Either
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.InvalidJson
import no.nav.helsemelding.message.error.SerializationError

class OutgoingDialogMessageSerializer(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun serialize(dialogMessage: OutgoingDialogMessage): Either<ConversionError, String> =
        Either.catch { json.encodeToString(dialogMessage) }
            .mapLeft { SerializationError("Could not serialize OutgoingDialogMessage to JSON", it) }

    fun deserialize(value: String): Either<ConversionError, OutgoingDialogMessage> =
        Either.catch { json.decodeFromString<OutgoingDialogMessage>(value) }
            .mapLeft { InvalidJson("Could not deserialize OutgoingDialogMessage JSON", it) }
}

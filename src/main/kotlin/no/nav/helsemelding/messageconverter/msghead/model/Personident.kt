package no.nav.helsemelding.messageconverter.msghead.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.helsemelding.messageconverter.error.MappingError

@Serializable(with = PersonidentSerializer::class)
@ConsistentCopyVisibility
data class Personident private constructor(val value: String) {
    companion object {
        val elevenDigits = Regex("^\\d{11}\$")

        operator fun invoke(value: String): Either<MappingError, Personident> =
            either {
                ensure(elevenDigits.matches(value)) {
                    MappingError(
                        message = "Value($value) is not a valid Personident",
                        field = "personident"
                    )
                }

                Personident(value)
            }
    }
}

fun Personident.isDNR() = this.value[0].digitToInt() > 3

object PersonidentSerializer : KSerializer<Personident> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Personident", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Personident) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Personident {
        val value = decoder.decodeString()
        return Personident(value).getOrElse { error ->
            throw SerializationException(error.message)
        }
    }
}

package no.nav.helsemelding.message.msghead.model.provider

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
import no.nav.helsemelding.message.error.MappingError

@Serializable(with = OrganisationNumberSerializer::class)
@ConsistentCopyVisibility
data class OrganisationNumber private constructor(val value: String) {
    companion object {
        val nineDigits = Regex("^\\d{9}\$")

        operator fun invoke(value: String): Either<MappingError, OrganisationNumber> =
            either {
                ensure(nineDigits.matches(value)) {
                    MappingError(
                        message = "$value is not a valid organisation number",
                        field = "provider.office.organisationNumber"
                    )
                }

                OrganisationNumber(value)
            }
    }

    override fun toString(): String = value
}

object OrganisationNumberSerializer : KSerializer<OrganisationNumber> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OrganisationNumber", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OrganisationNumber) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): OrganisationNumber {
        val value = decoder.decodeString()
        return OrganisationNumber(value).getOrElse { error ->
            throw SerializationException(error.message)
        }
    }
}

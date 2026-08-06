package no.nav.helsemelding.messageconverter.msghead.model.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsemelding.messageconverter.msghead.model.provider.serializer.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class ProviderOffice(
    val herId: Int?,
    @SerialName("navn") val name: String?,
    @SerialName("adresse") val address: String?,
    @SerialName("postnummer") val postalCode: String?,
    @SerialName("poststed") val city: String?,
    @SerialName("orgnummer") val organisationNumber: OrganisationNumber?,
    @SerialName("dialogmeldingEnabled") val dialogMessageEnabled: Boolean,
    @SerialName("dialogmeldingEnabledLocked") val dialogMessageEnabledLocked: Boolean,
    val system: String?,
    @SerialName("mottatt")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val receivedAt: OffsetDateTime
)

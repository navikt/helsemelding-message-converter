package no.nav.helsemelding.message.client.providerregistry.model

import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ProviderOffice(
    val herId: Int?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: OrganisationNumber?,
    val dialogmeldingEnabled: Boolean,
    val dialogmeldingEnabledLocked: Boolean,
    val system: String?,
    @Serializable(with = OffsetDateTimeSerializer::class) val mottatt: OffsetDateTime
)

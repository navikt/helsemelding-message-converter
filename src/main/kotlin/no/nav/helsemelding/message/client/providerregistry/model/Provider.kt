package no.nav.helsemelding.message.client.providerregistry.model

import kotlinx.serialization.Serializable
import no.nav.helsemelding.message.msghead.model.Personident
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

@Serializable
data class Provider(
    val behandlerRef: Uuid,
    val personident: Personident?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val herId: Int?,
    val hprId: Int?,
    val telefon: String?,
    val kontor: ProviderOffice,
    val kategori: ProviderCategori,
    @Serializable(with = OffsetDateTimeSerializer::class) val mottatt: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerializer::class) val invalidated: OffsetDateTime? = null,
    val suspendert: Boolean
)

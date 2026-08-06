package no.nav.helsemelding.messageconverter.msghead.model.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.helsemelding.messageconverter.msghead.model.Personident
import no.nav.helsemelding.messageconverter.msghead.model.provider.serializer.OffsetDateTimeSerializer
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

@Serializable
data class Provider(
    @SerialName("behandlerRef") val providerReference: Uuid,
    @SerialName("personident") val nationalIdentityNumber: Personident?,
    @SerialName("fornavn") val firstName: String,
    @SerialName("mellomnavn") val middleName: String?,
    @SerialName("etternavn") val lastName: String,
    val herId: Int?,
    val hprId: Int?,
    @SerialName("telefon") val phoneNumber: String?,
    @SerialName("kontor") val office: ProviderOffice,
    @SerialName("kategori") val category: ProviderCategory,
    @SerialName("mottatt")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val receivedAt: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerializer::class) val invalidated: OffsetDateTime? = null,
    @SerialName("suspendert") val suspended: Boolean
)

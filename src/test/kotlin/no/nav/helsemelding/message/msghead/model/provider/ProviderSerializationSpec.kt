package no.nav.helsemelding.message.msghead.model.provider

import arrow.core.getOrElse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.helsemelding.message.msghead.model.Personident
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

class ProviderSerializationSpec : StringSpec(
    {
        "should serialize and deserialize Provider with OffsetDateTime fields" {
            val provider = Provider(
                providerReference = Uuid.parse("75837362-2d8c-4f50-9ba5-961999bf1acc"),
                nationalIdentityNumber = Personident("13326920147").getOrElse { error ->
                    error(error.message)
                },
                firstName = "Kari",
                middleName = "Anne",
                lastName = "Hansen",
                herId = 654321,
                hprId = 7654321,
                phoneNumber = null,
                office = ProviderOffice(
                    herId = 54321,
                    name = "Legekontoret",
                    address = "Storgata 15",
                    postalCode = "0158",
                    city = "Oslo",
                    organisationNumber = OrganisationNumber("987654321").getOrElse { error ->
                        error(error.message)
                    },
                    dialogMessageEnabled = true,
                    dialogMessageEnabledLocked = false,
                    system = null,
                    receivedAt = OffsetDateTime.parse("2026-07-06T09:48:44.5727191+02:00")
                ),
                category = ProviderCategory.DOCTOR,
                receivedAt = OffsetDateTime.parse("2026-07-06T09:48:44.5727191+02:00"),
                invalidated = OffsetDateTime.parse("2026-07-07T09:48:44.5727191+02:00"),
                suspended = false
            )

            val json = Json.encodeToString(provider)
            val decoded = Json.decodeFromString<Provider>(json)

            decoded shouldBe provider
        }
    }
)

package no.nav.helsemelding.messageconverter.msghead.model.provider

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OrganisationNumberSpec : StringSpec(
    {
        "should create OrganisationNumber when value has nine digits" {
            val organisationNumber = OrganisationNumber("987654321").shouldBeRight()

            organisationNumber.value shouldBe "987654321"
        }

        "should serialize and deserialize as string" {
            val organisationNumber = OrganisationNumber("987654321").shouldBeRight()

            val json = Json.encodeToString(organisationNumber)
            val decoded = Json.decodeFromString<OrganisationNumber>(json)

            json shouldBe """"987654321""""
            decoded shouldBe organisationNumber
        }

        "should return MappingError when value is not nine digits" {
            val error = OrganisationNumber("98765432").shouldBeLeft()

            error.message shouldBe "98765432 is not a valid organisation number"
            error.field shouldBe "provider.office.organisationNumber"
            error.cause shouldBe null
        }

        "should fail deserialization when value is not nine digits" {
            val error = shouldThrow<SerializationException> {
                Json.decodeFromString<OrganisationNumber>(""""98765432"""")
            }

            error.message shouldBe "98765432 is not a valid organisation number"
        }
    }
)

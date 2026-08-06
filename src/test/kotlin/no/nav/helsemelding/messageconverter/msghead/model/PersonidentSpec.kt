package no.nav.helsemelding.messageconverter.msghead.model

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersonidentSpec : StringSpec(
    {
        "should create Personident when value has eleven digits" {
            val personident = Personident("12345678910").shouldBeRight()

            personident.value shouldBe "12345678910"
        }

        "should serialize and deserialize as string" {
            val personident = Personident("12345678910").shouldBeRight()

            val json = Json.encodeToString(personident)
            val decoded = Json.decodeFromString<Personident>(json)

            json shouldBe """"12345678910""""
            decoded shouldBe personident
        }

        "should return MappingError when value is not eleven digits" {
            val error = Personident("1234567891").shouldBeLeft()

            error.message shouldBe "Value(1234567891) is not a valid Personident"
            error.field shouldBe "personident"
            error.cause shouldBe null
        }

        "should fail deserialization when value is not eleven digits" {
            val error = shouldThrow<SerializationException> {
                Json.decodeFromString<Personident>(""""1234567891"""")
            }

            error.message shouldBe "Value(1234567891) is not a valid Personident"
        }
    }
)

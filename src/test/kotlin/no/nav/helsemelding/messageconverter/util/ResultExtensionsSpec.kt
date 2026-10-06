package no.nav.helsemelding.messageconverter.util

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Provider
import no.nav.helsemelding.jsonschema.core.model.ProviderOffice
import no.nav.helsemelding.jsonschema.core.model.Signature
import no.nav.helsemelding.messageconverter.error.ConversionException
import no.nav.helsemelding.messageconverter.error.InvalidJson

class ResultExtensionsSpec : StringSpec(
    {
        "should convert right value to successful Result" {
            val dialogMessage = IncomingDialogMessage(
                version = 1,
                id = "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                receivedAt = "2026-06-10T12:30",
                patientIdent = "12345678910",
                provider = Provider(
                    ident = "12345678910",
                    hprNumber = null,
                    office = ProviderOffice(orgNumber = null, orgName = "Office", herId = null)
                ),
                signature = Signature(
                    signingProviderIdent = "12345678910",
                    signedAt = "2026-06-10T12:30"
                ),
                documentId = "OD2510106934724",
                conversationReference = null,
                message = "Hei",
                numberOfAttachments = 0
            )

            val result = dialogMessage.right().toResult()

            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe dialogMessage
        }

        "should convert left ConversionError to failed Result" {
            val conversionError = InvalidJson("Could not deserialize DialogMessage JSON")

            val result = conversionError.left().toResult()

            result.isFailure shouldBe true
            val exception = result.exceptionOrNull().shouldBeInstanceOf<ConversionException>()
            exception.error shouldBe conversionError
        }

        "should support generic right values" {
            val result = "converted".right().toResult()

            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe "converted"
        }
    }
)

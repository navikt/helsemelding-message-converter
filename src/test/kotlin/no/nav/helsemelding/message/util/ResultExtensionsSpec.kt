package no.nav.helsemelding.message.util

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.message.error.ConversionException
import no.nav.helsemelding.message.error.InvalidJson

class ResultExtensionsSpec : StringSpec(
    {
        "should convert right value to successful Result" {
            val dialogMessage = IncomingDialogMessage(
                1,
                "dialog-1",
                IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                "2026-06-10T12:30",
                "12345678910",
                Sender(
                    "provider-1",
                    "signing-provider-1"
                ),
                null,
                "Hei",
                0
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

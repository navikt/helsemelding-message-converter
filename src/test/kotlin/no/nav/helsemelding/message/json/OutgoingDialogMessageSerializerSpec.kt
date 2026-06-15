package no.nav.helsemelding.message.json

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.error.InvalidJson

class OutgoingDialogMessageSerializerSpec : StringSpec(
    {
        val serializer = OutgoingDialogMessageSerializer()

        "should serialize OutgoingDialogMessage" {
            val dialogMessage = OutgoingDialogMessage(
                1,
                "dialog-1",
                "12345678910",
                "provider-1",
                ConversationReference(
                    "parent-1",
                    "conversation-1"
                ),
                OutgoingDialogMessageType.NAV_MESSAGE,
                "Hei",
                null
            )

            val json = serializer.serialize(dialogMessage).shouldBeRight()

            json shouldBe
                """{"version":1,"id":"dialog-1","patientIdent":"12345678910","providerId":"provider-1","conversationReference":{"parentMessageId":"parent-1","conversationId":"conversation-1"},"type":"NAV_MESSAGE","message":"Hei","attachment":null}"""
        }

        "should deserialize OutgoingDialogMessage and ignore unknown JSON fields" {
            val json = """
                {
                  "version": 1,
                  "id": "dialog-1",
                  "patientIdent": "12345678910",
                  "providerId": "provider-1",
                  "conversationReference": {
                    "parentMessageId": "parent-1",
                    "conversationId": "conversation-1"
                  },
                  "type": "NAV_MESSAGE",
                  "message": "Hei",
                  "attachment": null,
                  "ignored": "value"
                }
            """.trimIndent()

            serializer.deserialize(json).shouldBeRight(
                OutgoingDialogMessage(
                    1,
                    "dialog-1",
                    "12345678910",
                    "provider-1",
                    ConversationReference(
                        "parent-1",
                        "conversation-1"
                    ),
                    OutgoingDialogMessageType.NAV_MESSAGE,
                    "Hei",
                    null
                )
            )
        }

        "should return InvalidJson when JSON is malformed" {
            val error = serializer.deserialize("not-json").shouldBeLeft()

            error::class shouldBe InvalidJson::class
            error.message shouldBe "Could not deserialize OutgoingDialogMessage JSON"
        }
    }
)

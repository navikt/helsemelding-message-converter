package no.nav.helsemelding.message.json

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.error.InvalidJson

class OutgoingDialogMessageSerializerSpec : StringSpec(
    {
        val serializer = OutgoingDialogMessageSerializer()

        "should serialize OutgoingDialogMessage" {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                type = OutgoingDialogMessageType.NAV_MESSAGE,
                message = "Hei",
                attachment = null
            )

            val json = serializer.serialize(dialogMessage).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe Json.parseToJsonElement(
                """
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
                  "attachment": null
                }
                """.trimIndent()
            )
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
                    version = 1,
                    id = "dialog-1",
                    patientIdent = "12345678910",
                    providerId = "provider-1",
                    conversationReference = ConversationReference(
                        parentMessageId = "parent-1",
                        conversationId = "conversation-1"
                    ),
                    type = OutgoingDialogMessageType.NAV_MESSAGE,
                    message = "Hei",
                    attachment = null
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

package no.nav.helsemelding.message.json

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.message.error.InvalidJson

class IncomingDialogMessageSerializerSpec : StringSpec(
    {
        val serializer = IncomingDialogMessageSerializer()

        "should serialize IncomingDialogMessage" {
            val dialogMessage = IncomingDialogMessage(
                version = 1,
                id = "dialog-1",
                type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                receivedAt = "2026-06-10T12:30",
                patientIdent = "12345678910",
                sender = Sender(
                    providerId = "provider-1",
                    signingProviderId = "signing-provider-1"
                ),
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                message = "Hei",
                numberOfAttachments = 1
            )

            val json = serializer.serialize(dialogMessage).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe Json.parseToJsonElement(
                """
                {
                  "version": 1,
                  "id": "dialog-1",
                  "type": "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                  "receivedAt": "2026-06-10T12:30",
                  "patientIdent": "12345678910",
                  "sender": {
                    "providerId": "provider-1",
                    "signingProviderId": "signing-provider-1"
                  },
                  "conversationReference": {
                    "parentMessageId": "parent-1",
                    "conversationId": "conversation-1"
                  },
                  "message": "Hei",
                  "numberOfAttachments": 1
                }
                """.trimIndent()
            )
        }

        "should deserialize IncomingDialogMessage and ignore unknown JSON fields" {
            val json = """
                {
                  "version": 1,
                  "id": "dialog-1",
                  "type": "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                  "receivedAt": "2026-06-10T12:30",
                  "patientIdent": "12345678910",
                  "sender": {
                    "providerId": "provider-1",
                    "signingProviderId": "signing-provider-1"
                  },
                  "conversationReference": {
                    "parentMessageId": "parent-1",
                    "conversationId": "conversation-1"
                  },
                  "message": "Hei",
                  "numberOfAttachments": 1,
                  "ignored": "value"
                }
            """.trimIndent()

            serializer.deserialize(json).shouldBeRight(
                IncomingDialogMessage(
                    version = 1,
                    id = "dialog-1",
                    type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                    receivedAt = "2026-06-10T12:30",
                    patientIdent = "12345678910",
                    sender = Sender(
                        providerId = "provider-1",
                        signingProviderId = "signing-provider-1"
                    ),
                    conversationReference = ConversationReference(
                        parentMessageId = "parent-1",
                        conversationId = "conversation-1"
                    ),
                    message = "Hei",
                    numberOfAttachments = 1
                )
            )
        }

        "should return InvalidJson when JSON is malformed" {
            val error = serializer.deserialize("not-json").shouldBeLeft()

            error::class shouldBe InvalidJson::class
            error.message shouldBe "Could not deserialize IncomingDialogMessage JSON"
        }
    }
)

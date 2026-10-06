package no.nav.helsemelding.messageconverter.json

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Provider
import no.nav.helsemelding.jsonschema.core.model.ProviderOffice
import no.nav.helsemelding.jsonschema.core.model.Signature
import no.nav.helsemelding.messageconverter.error.InvalidJson

class IncomingDialogMessageSerializerSpec : StringSpec(
    {
        val serializer = IncomingDialogMessageSerializer()

        "should serialize IncomingDialogMessage" {
            val dialogMessage = IncomingDialogMessage(
                version = 1,
                id = "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                receivedAt = "2026-06-10T12:30",
                patientIdent = "12345678910",
                provider = Provider(
                    ident = "12345678910",
                    hprNumber = "123456",
                    office = ProviderOffice(orgNumber = null, orgName = "Office", herId = null)
                ),
                signature = Signature(
                    signingProviderIdent = "12345678910",
                    signedAt = "2026-06-10T12:30"
                ),
                documentId = "OD2510106934724",
                conversationReference = ConversationReference(
                    parentMessageId = "72c7b6a8-3abf-4c1b-9780-eb6eda94447a",
                    conversationId = "980a444b-c36b-49ab-90a3-8682ea31308d"
                ),
                message = "Hei",
                numberOfAttachments = 1
            )

            val json = serializer.serialize(dialogMessage).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe Json.parseToJsonElement(
                """
                {
                  "version": 1,
                  "id": "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                  "type": "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                  "receivedAt": "2026-06-10T12:30",
                  "patientIdent": "12345678910",
                  "provider": {
                    "ident": "12345678910",
                    "hprNumber": "123456",
                    "office": {
                      "orgNumber": null,
                      "orgName": "Office",
                      "herId": null
                    }
                  },
                  "signature": {
                    "signingProviderIdent": "12345678910",
                    "signedAt": "2026-06-10T12:30"
                  },
                  "documentId": "OD2510106934724",
                  "conversationReference": {
                    "parentMessageId": "72c7b6a8-3abf-4c1b-9780-eb6eda94447a",
                    "conversationId": "980a444b-c36b-49ab-90a3-8682ea31308d"
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
                  "id": "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                  "type": "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                  "receivedAt": "2026-06-10T12:30",
                  "patientIdent": "12345678910",
                  "provider": {
                    "ident": "12345678910",
                    "hprNumber": "123456",
                    "office": {
                      "orgNumber": null,
                      "orgName": "Office",
                      "herId": null
                    }
                  },
                  "signature": {
                    "signingProviderIdent": "12345678910",
                    "signedAt": "2026-06-10T12:30"
                  },
                  "documentId": "OD2510106934724",
                  "conversationReference": {
                    "parentMessageId": "72c7b6a8-3abf-4c1b-9780-eb6eda94447a",
                    "conversationId": "980a444b-c36b-49ab-90a3-8682ea31308d"
                  },
                  "message": "Hei",
                  "numberOfAttachments": 1,
                  "ignored": "value"
                }
            """.trimIndent()

            serializer.deserialize(json).shouldBeRight(
                IncomingDialogMessage(
                    version = 1,
                    id = "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                    type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                    receivedAt = "2026-06-10T12:30",
                    patientIdent = "12345678910",
                    provider = Provider(
                        ident = "12345678910",
                        hprNumber = "123456",
                        office = ProviderOffice(orgNumber = null, orgName = "Office", herId = null)
                    ),
                    signature = Signature(
                        signingProviderIdent = "12345678910",
                        signedAt = "2026-06-10T12:30"
                    ),
                    documentId = "OD2510106934724",
                    conversationReference = ConversationReference(
                        parentMessageId = "72c7b6a8-3abf-4c1b-9780-eb6eda94447a",
                        conversationId = "980a444b-c36b-49ab-90a3-8682ea31308d"
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

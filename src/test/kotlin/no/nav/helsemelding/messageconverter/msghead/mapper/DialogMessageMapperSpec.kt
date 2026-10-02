package no.nav.helsemelding.messageconverter.msghead.mapper

import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.dialogmelding.CV
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.dialogmelding.XMLNotat
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLConversationRef
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Provider
import no.nav.helsemelding.jsonschema.core.model.ProviderOffice
import no.nav.helsemelding.jsonschema.core.model.Signature
import no.nav.helsemelding.messageconverter.createProvider
import no.nav.helsemelding.messageconverter.error.AttachmentError
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.msghead.MSG_TYPE_DIALOG_NOTE
import no.nav.helsemelding.messageconverter.msghead.XmlSerializer
import no.nav.helsemelding.messageconverter.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.messageconverter.msghead.model.Employee
import no.nav.helsemelding.messageconverter.msghead.model.Personident
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.uuid.Uuid

class DialogMessageMapperSpec : StringSpec(
    {
        val mapper = DialogMessageMapper(Clock.fixed(Instant.parse("2026-10-02T09:00:00Z"), ZoneOffset.UTC))

        "should map incoming fields" {
            val msgHead = msgHead(
                msgId = "dialog-1",
                genDate = LocalDateTime.parse("2026-06-10T12:30:00"),
                patientId = "12345678910",
                providerId = "provider-1"
            )

            val dialogMessage = mapper.toIncomingDialogMessage(msgHead).shouldBeRight()

            dialogMessage shouldBe IncomingDialogMessage(
                version = 1,
                id = "dialog-1",
                type = IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                receivedAt = "2026-10-02T09:00:00Z",
                patientIdent = "12345678910",
                provider = Provider(
                    ident = "12345678910",
                    hprNumber = "123456",
                    office = ProviderOffice(orgNumber = "provider-1", orgName = "Office", herId = "100")
                ),
                signature = Signature(signingProviderIdent = "12345678910", signedAt = "2026-06-10T12:30"),
                documentId = "doc-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                message = "",
                numberOfAttachments = 0
            )
        }

        "should use the current UTC instant as receivedAt" {
            val before = Instant.now()
            val result = DialogMessageMapper().toIncomingDialogMessage(msgHead()).shouldBeRight()
            val after = Instant.now()
            val receivedAt = Instant.parse(result.receivedAt)

            receivedAt.isBefore(before) shouldBe false
            receivedAt.isAfter(after) shouldBe false
        }

        "should map provider D-number" {
            val msgHead = msgHead()
            val identifiers = msgHead.msgInfo.sender.organisation.healthcareProfessional.ident
            identifiers.removeAll { it.typeId?.v == "FNR" }
            identifiers.add(identifier("42345678910", "DNR"))

            val result = mapper.toIncomingDialogMessage(msgHead).shouldBeRight()

            result.provider.ident shouldBe "42345678910"
            result.signature.signingProviderIdent shouldBe "42345678910"
        }

        "should map missing optional information to null" {
            val msgHead = msgHead()
            val organisation = msgHead.msgInfo.sender.organisation
            organisation.ident.clear()
            organisation.healthcareProfessional.ident.removeAll { it.typeId?.v == "HPR" }
            msgHead.msgInfo.conversationRef = null

            val result = mapper.toIncomingDialogMessage(msgHead).shouldBeRight()

            result.provider shouldBe Provider(
                ident = "12345678910",
                hprNumber = null,
                office = ProviderOffice(orgNumber = null, orgName = "Office", herId = null)
            )
            result.conversationReference shouldBe null
        }

        withData(
            nameFn = { "should reject message with missing mandatory field: $it" },
            listOf("providerIdent", "officeName", "documentId")
        ) { field ->
            val msgHead = msgHead()
            when (field) {
                "providerIdent" -> msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.clear()
                "officeName" -> msgHead.msgInfo.sender.organisation.organisationName = null
                "documentId" -> {
                    val dialog = msgHead.document.first().refDoc.content.any.first() as XMLDialogmelding
                    dialog.notat.first().dokIdNotat = null
                }
            }

            mapper.toIncomingDialogMessage(msgHead).shouldBeLeft().shouldBeInstanceOf<MappingError>()
        }

        "should always report 0 attachments" {
            val msgHead = msgHead()
            msgHead.document.add(
                XMLDocument().apply {
                    refDoc = XMLRefDoc().apply {
                        msgType = XMLCS().apply { v = "A" }
                        mimeType = "application/pdf"
                    }
                }
            )

            mapper.toIncomingDialogMessage(msgHead).shouldBeRight().numberOfAttachments shouldBe 0
        }

        withData(
            nameFn = { "should map outgoing type=$it" },
            OutgoingDialogMessageType.entries.toTypedArray().toList()
        ) {
            val path = "src/test/resources/outgoing/${it.name}.xml"
            val messageXml = Files.readString(Paths.get(path))
            val xmlSerializer = XmlSerializer()
            val patientIdent = Personident("24274116206").getOrElse { error ->
                error(error.message)
            }
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = Uuid.parse("dbb4a1cb-943e-4bbb-967d-eb7ef456a30f"),
                patientIdent = patientIdent.toString(),
                providerId = "e5d65352-2fa1-49b0-be3a-a7fd26208998",
                conversationReference = ConversationReference(
                    parentMessageId = "2eacbe5e-a087-4239-934c-6a1af772e91c",
                    conversationId = "980a444b-c36b-49ab-90a3-8682ea31308d"
                ),
                type = it,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )
            val provider = createProvider(Uuid.random())

            val employee = Employee(
                firstName = "Ola",
                middleName = "Jens",
                lastName = "Nordmann",
                personident = patientIdent
            )

            val additionalInfo = AdditionalMessageInfo(
                provider = provider,
                employee = employee,
                createdAt = Instant.parse("2026-07-06T07:48:44.572719100Z"),
                docId = Uuid.parse("769a5524-ca26-4d57-a0f4-d0a1d8f445c9")
            )

            val outgoingMessage = createOutgoingMessage(dialogMessage, additionalInfo).shouldBeRight()
            val msgHead = mapper.toMsgHead(outgoingMessage).shouldBeRight()

            val serialized = xmlSerializer.serialize(msgHead).shouldBeRight()

            serialized.noLineBreaks() shouldBe messageXml.noLineBreaks()
        }

        "should reject missing msgInfo" {
            val error = mapper.toIncomingDialogMessage(XMLMsgHead()).shouldBeLeft() as MappingError

            error.message shouldBe "Missing required MsgHead field: msgInfo.msgId"
            error.field shouldBe "msgInfo.msgId"
            error.cause shouldBe null
        }

        "should reject invalid attachment" {
            val patientIdent = Personident("24274116206").getOrElse { error ->
                error(error.message)
            }
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = Uuid.parse("dbb4a1cb-943e-4bbb-967d-eb7ef456a30f"),
                patientIdent = patientIdent.toString(),
                providerId = "e5d65352-2fa1-49b0-be3a-a7fd26208998",
                conversationReference = ConversationReference(
                    parentMessageId = "2eacbe5e-a087-4239-934c-6a1af772e91c",
                    conversationId = "980a444b-c36b-49ab-90a3-8682ea31308d"
                ),
                type = OutgoingDialogMessageType.NAV_MESSAGE,
                message = "Hei",
                attachment = "not-base64"
            )
            val additionalInfo = AdditionalMessageInfo(
                provider = createProvider(Uuid.random()),
                employee = Employee(
                    firstName = "Ola",
                    middleName = "Jens",
                    lastName = "Nordmann",
                    personident = patientIdent
                ),
                createdAt = Instant.parse("2026-07-06T07:48:44.572719100Z"),
                docId = Uuid.parse("769a5524-ca26-4d57-a0f4-d0a1d8f445c9")
            )

            val outgoingMessage = createOutgoingMessage(dialogMessage, additionalInfo).shouldBeRight()
            val error = mapper.toMsgHead(outgoingMessage).shouldBeLeft()

            error.shouldBeInstanceOf<AttachmentError>()
            error.message shouldBe "Could not decode base64 attachment"
            error.cause shouldNotBe null
        }
    }
)

private fun String.noLineBreaks(): String = this.replace("\r", "").replace("\n", "")

private fun msgHead(
    msgId: String = "dialog-1",
    genDate: LocalDateTime = LocalDateTime.parse("2025-01-01T12:30:00"),
    patientId: String = "12345678910",
    providerId: String = "provider-1",
    typeV: String = MSG_TYPE_DIALOG_NOTE
): XMLMsgHead =
    XMLMsgHead().apply {
        msgInfo = XMLMsgInfo().apply {
            this.msgId = msgId
            this.genDate = genDate
            type = XMLCS().apply { v = typeV }
            sender = XMLSender().apply {
                organisation = XMLOrganisation().apply {
                    organisationName = "Office"
                    ident.add(identifier(providerId, "ENH"))
                    ident.add(identifier("100", "HER"))
                    healthcareProfessional = XMLHealthcareProfessional().apply {
                        ident.add(identifier("200", "HER"))
                        ident.add(identifier("123456", "HPR"))
                        ident.add(identifier("12345678910", "FNR"))
                    }
                }
            }
            conversationRef = XMLConversationRef().apply {
                refToParent = "parent-1"
                refToConversation = "conversation-1"
            }
            patient = XMLPatient().apply {
                ident.add(identifier(patientId, "FNR"))
            }
        }
        document.add(
            XMLDocument().apply {
                refDoc = XMLRefDoc().apply {
                    msgType = XMLCS().apply { v = "XML" }
                    content = XMLRefDoc.Content().apply {
                        any.add(
                            XMLDialogmelding().apply {
                                notat.add(
                                    XMLNotat().apply {
                                        dokIdNotat = "doc-1"
                                        temaKodet = CV().apply {
                                            v = "1"
                                            s = "2.16.578.1.12.4.1.1.8128"
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        )
    }

private fun identifier(value: String, type: String): XMLIdent =
    XMLIdent().apply {
        id = value
        typeId = XMLCV().apply { v = type }
    }

package no.nav.helsemelding.message.msghead

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import no.nav.helse.msgHead.XMLConversationRef
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLSender
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.IncomingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.Sender
import no.nav.helsemelding.message.converter.createProvider
import no.nav.helsemelding.message.error.MappingError
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.Arbeidstaker
import no.nav.helsemelding.message.msghead.model.Personident
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class MsgHeadDialogMessageMapperSpec : StringSpec(
    {
        val mapper = MsgHeadDialogMessageMapper()

        "maps required MsgHead fields to IncomingDialogMessage" {
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
                receivedAt = "2026-06-10T12:30",
                patientIdent = "12345678910",
                sender = Sender(
                    providerId = "provider-1",
                    signingProviderId = "provider-1"
                ),
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                message = "",
                numberOfAttachments = 0
            )
        }

        withData(
            nameFn = { "maps OutgoingDialogMessage(type=$it) to MsgHead" },
            OutgoingDialogMessageType.entries.toTypedArray().toList()
        ) {
            val path = "src/test/resources/outgoing/${it.name}.xml"
            val messageXml = Files.readString(Paths.get(path))
            val xmlSerializer = XmlSerializer()
            val patientIdent = Personident("24274116206")
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dbb4a1cb-943e-4bbb-967d-eb7ef456a30f",
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

            val arbeidstaker = Arbeidstaker(
                fornavn = "Ola",
                mellomnavn = "Jens",
                etternavn = "Nordmann",
                personident = patientIdent
            )

            val additionalInfo = AdditionalMessageInfo(
                provider = provider,
                arbeidstaker = arbeidstaker,
                createdAt = LocalDateTime.parse("2026-07-06T09:48:44.5727191"),
                dokId = Uuid.parse("769a5524-ca26-4d57-a0f4-d0a1d8f445c9")
            )

            val outgoingMessage = dialogMessage.toOutgoingMessage(additionalInfo).shouldBeRight()
            val msgHead = mapper.toMsgHead(outgoingMessage).shouldBeRight()

            val serialized = xmlSerializer.serialize(msgHead).shouldBeRight()

            serialized.noLineBreaks() shouldBe messageXml.noLineBreaks()
        }

        "returns MappingError when MsgHead lacks msgInfo" {
            val error = mapper.toIncomingDialogMessage(XMLMsgHead()).shouldBeLeft() as MappingError

            error.message shouldBe "Missing required MsgHead field: msgInfo.msgId"
            error.field shouldBe "msgInfo.msgId"
            error.cause shouldBe null
        }
    }
)

private fun String.noLineBreaks(): String = this.replace("\r", "").replace("\n", "")

private fun msgHead(
    msgId: String,
    genDate: LocalDateTime,
    patientId: String,
    providerId: String
): XMLMsgHead =
    XMLMsgHead().apply {
        msgInfo = XMLMsgInfo().apply {
            this.msgId = msgId
            this.genDate = genDate
            sender = XMLSender().apply {
                organisation = XMLOrganisation().apply {
                    ident.add(XMLIdent().apply { id = providerId })
                }
            }
            conversationRef = XMLConversationRef().apply {
                refToParent = "parent-1"
                refToConversation = "conversation-1"
            }
            patient = XMLPatient().apply {
                ident.add(XMLIdent().apply { id = patientId })
            }
        }
    }

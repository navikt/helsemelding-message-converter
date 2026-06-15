package no.nav.helsemelding.message.msghead

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
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
import no.nav.helsemelding.message.error.MappingError
import java.time.LocalDateTime

class MsgHeadDialogMessageMapperSpec : StringSpec(
    {
        val mapper = MsgHeadDialogMessageMapper()

        "maps required MsgHead fields to DialogMessage" {
            val msgHead = msgHead(
                msgId = "dialog-1",
                genDate = LocalDateTime.parse("2026-06-10T12:30:00"),
                patientId = "12345678910",
                providerId = "provider-1"
            )

            val dialogMessage = mapper.toIncomingDialogMessage(msgHead).shouldBeRight()

            dialogMessage shouldBe IncomingDialogMessage(
                1,
                "dialog-1",
                IncomingDialogMessageType.SICK_LEAVE_FOLLOW_UP_INQUIRY,
                "2026-06-10T12:30",
                "12345678910",
                Sender(
                    "provider-1",
                    "provider-1"
                ),
                ConversationReference(
                    "parent-1",
                    "conversation-1"
                ),
                "",
                0
            )
        }

        "maps OutgoingDialogMessage to MsgHead" {
            val msgHead = mapper.toMsgHead(
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
            ).shouldBeRight()

            msgHead.msgInfo.msgId shouldBe "dialog-1"
            msgHead.msgInfo.patient.ident.first().id shouldBe "12345678910"
            msgHead.msgInfo.sender.organisation.ident.first().id shouldBe "provider-1"
            msgHead.msgInfo.conversationRef.refToParent shouldBe "parent-1"
            msgHead.msgInfo.conversationRef.refToConversation shouldBe "conversation-1"
            msgHead.document.first().refDoc.content.any.first() shouldBe "Hei"
        }

        "returns MappingError when MsgHead lacks msgInfo" {
            val error = mapper.toIncomingDialogMessage(XMLMsgHead()).shouldBeLeft() as MappingError

            error.message shouldBe "Missing required MsgHead field: msgInfo.msgId"
            error.field shouldBe "msgInfo.msgId"
            error.cause shouldBe null
        }
    }
)

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

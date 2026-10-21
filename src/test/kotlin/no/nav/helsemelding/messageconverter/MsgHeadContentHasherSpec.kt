package no.nav.helsemelding.messageconverter

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.base64container.Base64Container
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.msghead.XmlSerializer
import java.nio.charset.StandardCharsets.UTF_8

class MsgHeadContentHasherSpec : StringSpec(
    {
        val converter = MsgHeadMessageConverter()

        "should produce a repeatable hash with 64 lowercase hexadecimal characters" {
            val xml = msgHeadXml()

            val hash = converter.calculateContentHash(xml).shouldBeRight()

            converter.calculateContentHash(xml).shouldBeRight() shouldBe hash
            hash.matches(Regex("[0-9a-f]{64}")) shouldBe true
        }

        "should change the hash when dialog message content changes" {
            val oldHash = converter.calculateContentHash(msgHeadXml()).shouldBeRight()

            val newHash = converter.calculateContentHash(msgHeadXml(dialogText = "changed dialog")).shouldBeRight()
            newHash shouldNotBe oldHash
        }

        "should change the hash when patient content changes" {
            val oldHash = converter.calculateContentHash(msgHeadXml()).shouldBeRight()

            val newHash = converter.calculateContentHash(msgHeadXml(patientIdent = "10987654321")).shouldBeRight()
            newHash shouldNotBe oldHash
        }

        "should change the hash when attachment bytes, order or count changes" {
            val attachment1 = attachment(description = "one", bytes = "first".toByteArray(UTF_8))
            val attachment2 = attachment(description = "two", bytes = "second".toByteArray(UTF_8))

            val changedAttachments = listOf(attachment1, attachment("two", "changed".toByteArray(UTF_8)))
            val swappedAttachments = listOf(attachment2, attachment1)
            val extraAttachments = listOf(attachment1, attachment2, attachment("three", "third".toByteArray(UTF_8)))
            val missingAttachments = listOf(attachment1)

            val oldHash = converter.calculateContentHash(msgHeadXml(attachments = listOf(attachment1, attachment2))).shouldBeRight()
            val changedAttachmentsHash = converter.calculateContentHash(msgHeadXml(attachments = changedAttachments)).shouldBeRight()
            val swappedAttachmentsHash = converter.calculateContentHash(msgHeadXml(attachments = swappedAttachments)).shouldBeRight()
            val extraAttachmentsHash = converter.calculateContentHash(msgHeadXml(attachments = extraAttachments)).shouldBeRight()
            val missingAttachmentsHash = converter.calculateContentHash(msgHeadXml(attachments = missingAttachments)).shouldBeRight()

            changedAttachmentsHash shouldNotBe oldHash
            swappedAttachmentsHash shouldNotBe oldHash
            extraAttachmentsHash shouldNotBe oldHash
            missingAttachmentsHash shouldNotBe oldHash
        }

        "should return MappingError when dialog message is missing" {
            val missingDialog = converter.calculateContentHash(msgHeadXml(dialogText = null)).shouldBeLeft()

            missingDialog.shouldBeInstanceOf<MappingError>().field shouldBe "document[0].refDoc.content.Dialogmelding"
        }

        "should return MappingError when patient is missing" {
            val missingPatient = converter.calculateContentHash(msgHeadXml(patientIdent = null)).shouldBeLeft()

            missingPatient.shouldBeInstanceOf<MappingError>().field shouldBe "msgInfo.patient"
        }

        "should support nested MsgHead" {
            val xml = msgHeadXml(dialogText = null, patientIdent = null, nested = msgHead())

            val nestedMessageHash = converter.calculateContentHash(xml).shouldBeRight()
            nestedMessageHash.matches(Regex("[0-9a-f]{64}")) shouldBe true
        }
    }
)

private fun msgHeadXml(
    id: String = "fe668a4a-13f3-4035-a367-39b76ade7ea8",
    patientIdent: String? = "12345678910",
    dialogText: String? = "dialog text",
    attachments: List<XMLDocument> = emptyList(),
    nested: XMLMsgHead? = null
): String {
    val msgHead = msgHead(id, patientIdent, dialogText, attachments, nested)
    return XmlSerializer().serialize(msgHead).shouldBeRight()
}

private fun msgHead(
    id: String = "45ea846c-f21c-40c9-b0d8-af5ba7692e98",
    patientIdent: String? = "12345678910",
    dialogText: String? = "dialog text",
    attachments: List<XMLDocument> = emptyList(),
    nested: XMLMsgHead? = null
): XMLMsgHead =
    XMLMsgHead().apply {
        msgInfo = XMLMsgInfo().apply {
            msgId = id
            patient = patientIdent?.let(::patient)
        }
        if (dialogText != null) {
            document.add(
                XMLDocument().apply {
                    refDoc = XMLRefDoc().apply {
                        msgType = XMLCS().apply { v = "XML" }
                        content = XMLRefDoc.Content().apply {
                            any.add(XMLDialogmelding().apply { sakstype = dialogText })
                        }
                    }
                }
            )
        }
        document.addAll(attachments)
        nested?.let { child ->
            document.add(
                XMLDocument().apply {
                    refDoc = XMLRefDoc().apply {
                        msgType = XMLCS().apply { v = "XML" }
                        content = XMLRefDoc.Content().apply { any.add(child) }
                    }
                }
            )
        }
    }

private fun patient(patientIdent: String): XMLPatient =
    XMLPatient().apply {
        familyName = "Patient"
        ident.add(
            XMLIdent().apply {
                id = patientIdent
                typeId = XMLCV().apply { v = "FNR" }
            }
        )
    }

private fun attachment(description: String, bytes: ByteArray): XMLDocument =
    XMLDocument().apply {
        refDoc = XMLRefDoc().apply {
            msgType = XMLCS().apply { v = "A" }
            mimeType = "application/pdf"
            this.description = description
            content = XMLRefDoc.Content().apply {
                any.add(Base64Container().apply { value = bytes })
            }
        }
    }

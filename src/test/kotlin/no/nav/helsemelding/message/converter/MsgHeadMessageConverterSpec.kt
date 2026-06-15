package no.nav.helsemelding.message.converter

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helsemelding.message.error.InvalidJson
import no.nav.helsemelding.message.error.InvalidXml
import no.nav.helsemelding.message.msghead.XmlSerializer
import java.nio.file.Files
import java.nio.file.Paths

private const val XML_MESSAGE_WITH_ATTACHMENTS_PATH = "src/test/resources/message_with_attachments.xml"
private const val XML_MESSAGE_WITHOUT_ATTACHMENTS_PATH = "src/test/resources/message_without_attachments.xml"

class MsgHeadMessageConverterSpec : StringSpec(
    {
        val converter = MsgHeadMessageConverter()
        val serializer = XmlSerializer()

        "should convert MsgHead XML to DialogMessage JSON" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH)))

            val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

            json shouldContain "\"version\":1"
            json shouldContain "\"id\":\"df978545-189c-4ad2-8479-d5271d69e0b6\""
            json shouldContain "\"type\":\"SICK_LEAVE_FOLLOW_UP_INQUIRY\""
            json shouldContain "\"receivedAt\":\"2026-05-29T13:13:28.967022541\""
            json shouldContain "\"patientIdent\":\"31777207884\""
            json shouldContain "\"numberOfAttachments\":3"
        }

        "should return InvalidXml when MsgHead XML is malformed" {
            val error = converter.incomingDialogMessageXmlToJson("not-xml").shouldBeLeft()

            error.shouldBeInstanceOf<InvalidXml>()
            error.message shouldBe "Could not deserialize MsgHead XML"
        }

        "should return InvalidJson when outgoing DialogMessage JSON is malformed" {
            val error = converter.outgoingDialogMessageJsonToXml("not-json").shouldBeLeft()

            error.shouldBeInstanceOf<InvalidJson>()
            error.message shouldBe "Could not deserialize OutgoingDialogMessage JSON"
        }

        "should extract attachments from MsgHead XML" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH)))

            val attachments = converter.extractAttachments(messageXml).shouldBeRight()

            attachments.size shouldBe 3
            attachments.map { it.description } shouldContainExactly listOf(
                "Testvedlegg 1",
                "Testvedlegg 2",
                "Testvedlegg 3"
            )
            attachments.map { it.contentType }.distinct() shouldContainExactly listOf("application/pdf")
        }

        "should return empty attachment list when MsgHead XML has no attachments" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_WITHOUT_ATTACHMENTS_PATH)))

            converter.extractAttachments(messageXml).shouldBeRight() shouldBe emptyList()
        }

        "should remove attachments from MsgHead XML" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH)))

            val xmlWithoutAttachments = converter.removeAttachments(messageXml).shouldBeRight()
            val msgHead = serializer.deserialize(xmlWithoutAttachments).shouldBeRight()

            msgHead.document.size shouldBe 1
            xmlWithoutAttachments shouldContain "MsgHead"
        }
    }
)

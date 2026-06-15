package no.nav.helsemelding.message.msghead

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helsemelding.message.error.InvalidXml
import no.nav.helsemelding.message.error.SerializationError
import org.xml.sax.SAXParseException
import java.nio.file.Files
import java.nio.file.Paths

private const val XML_MESSAGE_PATH = "src/test/resources/message_with_attachments.xml"
private const val XML_MESSAGE_WITH_DOCTYPE_PATH = "src/test/resources/message_with_doctype.xml"

class XmlSerializerSpec : StringSpec(
    {
        val serializer = XmlSerializer()

        "should deserialize XML message to XMLMsgHead" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_PATH)))

            val msgHead = serializer.deserialize(messageXml).shouldBeRight()

            msgHead shouldNotBe null
            msgHead::class shouldBe XMLMsgHead::class
            msgHead.msgInfo shouldNotBe null
            msgHead.document.size shouldBe 4
        }

        "should serialize XMLMsgHead to XML string" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_PATH)))
            val msgHead = serializer.deserialize(messageXml).shouldBeRight()
            val serializedXml = serializer.serialize(msgHead).shouldBeRight()

            serializedXml shouldContain "MsgHead"
            serializedXml shouldContain "MsgInfo"
            serializedXml shouldContain "Base64Container"
        }

        "should deserialize serialized XML message" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_PATH)))
            val msgHead = serializer.deserialize(messageXml).shouldBeRight()

            val serializedXml = serializer.serialize(msgHead).shouldBeRight()
            val deserializedMsgHead = serializer.deserialize(serializedXml).shouldBeRight()

            deserializedMsgHead.msgInfo shouldNotBe null
            deserializedMsgHead.document.size shouldBe msgHead.document.size
        }

        "should return SerializationError when MsgHead contains unsupported XML content" {
            val msgHead = XMLMsgHead().apply {
                msgInfo = XMLMsgInfo().apply { msgId = "dialog-1" }
                document.add(
                    XMLDocument().apply {
                        refDoc = XMLRefDoc().apply {
                            content = XMLRefDoc.Content().apply {
                                any.add(Any())
                            }
                        }
                    }
                )
            }

            val error = serializer.serialize(msgHead).shouldBeLeft()

            error::class shouldBe SerializationError::class
            error.message shouldBe "Could not serialize MsgHead XML"
        }

        "should reject XML with doctype declaration" {
            val messageXml = String(Files.readAllBytes(Paths.get(XML_MESSAGE_WITH_DOCTYPE_PATH)))

            val error = serializer.deserialize(messageXml).shouldBeLeft()

            error::class shouldBe InvalidXml::class
            val rootException = error.cause?.cause!!
            rootException::class shouldBe SAXParseException::class
            rootException.message shouldContain "DOCTYPE is disallowed"
        }
    }
)

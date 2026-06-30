package no.nav.helsemelding.message.msghead

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.helse.base64container.Base64Container
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helsemelding.message.error.AttachmentError

class ExtensionsSpec : StringSpec(
    {
        "should map attachment document to Attachment" {
            val document = attachmentDocument(
                description = "Vedlegg",
                mimeType = "application/pdf",
                content = Base64Container().apply { value = "content".toByteArray() }
            )

            val attachment = document.toAttachment().shouldBeRight()

            attachment.description shouldBe "Vedlegg"
            attachment.contentType shouldBe "application/pdf"
            attachment.contentBase64 shouldBe "Y29udGVudA=="
        }

        "should return AttachmentError when attachment content is not Base64Container" {
            val document = attachmentDocument(
                description = "Vedlegg",
                mimeType = "application/pdf",
                content = "not-base64-container"
            )

            val error = document.toAttachment().shouldBeLeft()

            error::class shouldBe AttachmentError::class
            error.message shouldBe "Could not map MsgHead document to attachment"
        }
    }
)

private fun attachmentDocument(
    description: String,
    mimeType: String,
    content: Any
): XMLDocument =
    XMLDocument().apply {
        refDoc = XMLRefDoc().apply {
            msgType = XMLCS().apply { v = ATTACHMENT_TYPE }
            this.mimeType = mimeType
            this.description = description
            this.content = XMLRefDoc.Content().apply {
                any.add(content)
            }
        }
    }

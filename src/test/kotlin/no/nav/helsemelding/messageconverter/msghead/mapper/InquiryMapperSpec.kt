package no.nav.helsemelding.messageconverter.msghead.mapper

import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.helse.base64container.Base64Container
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.messageconverter.createProvider
import no.nav.helsemelding.messageconverter.msghead.model.Employee
import no.nav.helsemelding.messageconverter.msghead.model.InquiryMessage
import no.nav.helsemelding.messageconverter.msghead.model.Personident
import java.time.Instant
import kotlin.uuid.Uuid

class InquiryMapperSpec : StringSpec(
    {
        "should map inquiry with attachment" {
            val message = inquiryMessage()

            val msgHead = createInquiry(message).shouldBeRight()

            msgHead.msgInfo.type.v shouldBe "DIALOG_FORESPORSEL"
            msgHead.msgInfo.type.dn shouldBe "Forespørsel"
            msgHead.msgInfo.conversationRef.refToParent shouldBe "parent-1"
            msgHead.msgInfo.conversationRef.refToConversation shouldBe "conversation-1"
            msgHead.msgInfo.receiver.organisation.healthcareProfessional.roleToPatient shouldBe null
            msgHead.document.size shouldBe 2

            val dialogMessage = msgHead.document[0]
                .refDoc
                .content
                .any
                .first()
                .shouldBeTypeOf<XMLDialogmelding>()
            val inquiry = dialogMessage.foresporsel.single()

            inquiry.typeForesp.v shouldBe message.type.code.toString()
            inquiry.typeForesp.dn shouldBe message.type.application
            inquiry.sporsmal shouldBe "Hei"
            inquiry.dokIdForesp shouldBe "769a5524-ca26-4d57-a0f4-d0a1d8f445c9"

            val attachment = msgHead.document[1]
                .refDoc
                .content
                .any
                .first()
                .shouldBeTypeOf<Base64Container>()

            attachment.value.decodeToString() shouldBe "Base64 encoded file"
        }
    }
)

private fun inquiryMessage(): InquiryMessage {
    val patientIdent = Personident("24274116206").getOrElse { error ->
        error(error.message)
    }

    return InquiryMessage(
        id = "dialog-1",
        conversationReference = ConversationReference(
            parentMessageId = "parent-1",
            conversationId = "conversation-1"
        ),
        type = OutgoingDialogMessageType.PATIENT_REQUEST,
        message = "Hei",
        attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ==",
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
}

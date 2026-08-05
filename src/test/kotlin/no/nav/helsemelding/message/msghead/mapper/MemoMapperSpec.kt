package no.nav.helsemelding.message.msghead.mapper

import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.converter.createProvider
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.Personident
import java.time.Instant
import kotlin.uuid.Uuid

class MemoMapperSpec : StringSpec(
    {
        "should map memo with reference" {
            val message = memoMessage()

            val msgHead = createMemo(message).shouldBeRight()

            msgHead.msgInfo.type.v shouldBe "DIALOG_NOTAT"
            msgHead.msgInfo.type.dn shouldBe "Notat"
            msgHead.msgInfo.conversationRef.refToParent shouldBe "parent-1"
            msgHead.msgInfo.conversationRef.refToConversation shouldBe "conversation-1"
            msgHead.msgInfo.receiver.organisation.healthcareProfessional.roleToPatient shouldBe null
            msgHead.document.size shouldBe 1

            val dialogMessage = msgHead.document.single()
                .refDoc
                .content
                .any
                .first()
                .shouldBeTypeOf<XMLDialogmelding>()
            val memo = dialogMessage.notat.single()

            memo.temaKodet.v shouldBe message.type.code.toString()
            memo.temaKodet.dn shouldBe message.type.application
            memo.tekstNotatInnhold shouldBe "Hei"
            memo.dokIdNotat shouldBe "769a5524-ca26-4d57-a0f4-d0a1d8f445c9"
        }
    }
)

private fun memoMessage(): MemoMessage {
    val patientIdent = Personident("24274116206").getOrElse { error ->
        error(error.message)
    }

    return MemoMessage(
        id = "dialog-1",
        conversationReference = ConversationReference(
            parentMessageId = "parent-1",
            conversationId = "conversation-1"
        ),
        type = OutgoingDialogMessageType.NAV_MESSAGE,
        message = "Hei",
        attachment = null,
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

package no.nav.helsemelding.message.msghead.mapper

import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.converter.createProvider
import no.nav.helsemelding.message.error.AttachmentError
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.Personident
import java.time.Instant
import kotlin.uuid.Uuid

class FollowUpPlanMapperSpec : StringSpec(
    {
        "should map follow-up plan" {
            val message = followUpPlanMessage()

            val msgHead = createFollowUpPlan(message).shouldBeRight()

            msgHead.msgInfo.type.v shouldBe "DIALOG_NOTAT"
            msgHead.msgInfo.type.dn shouldBe "Notat"
            msgHead.msgInfo.conversationRef shouldBe null
            msgHead.msgInfo.receiver.organisation.healthcareProfessional.roleToPatient.v shouldBe "6"
            msgHead.document.size shouldBe 2

            val dialogMessage = msgHead.document[0]
                .refDoc
                .content
                .any
                .first()
                .shouldBeTypeOf<XMLDialogmelding>()
            val followUpPlan = dialogMessage.notat.single()

            followUpPlan.temaKodet.v shouldBe message.type.code.toString()
            followUpPlan.temaKodet.dn shouldBe message.type.application
            followUpPlan.tekstNotatInnhold shouldBe "Åpne PDF-vedlegg"
            followUpPlan.dokIdNotat shouldBe "769a5524-ca26-4d57-a0f4-d0a1d8f445c9"
            followUpPlan.rollerRelatertNotat.single().rolleNotat.v shouldBe "1"
        }

        "should reject invalid attachment" {
            val message = followUpPlanMessage(attachment = "not-base64")

            val error = createFollowUpPlan(message).shouldBeLeft()

            error.shouldBeInstanceOf<AttachmentError>()
            error.message shouldBe "Could not decode base64 attachment"
            error.cause shouldNotBe null
        }
    }
)

private fun followUpPlanMessage(
    attachment: String = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
): FollowUpPlanMessage {
    val patientIdent = Personident("24274116206").getOrElse { error ->
        error(error.message)
    }

    return FollowUpPlanMessage(
        id = "dialog-1",
        type = OutgoingDialogMessageType.FOLLOW_UP_PLAN,
        message = "Åpne PDF-vedlegg",
        attachment = attachment,
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

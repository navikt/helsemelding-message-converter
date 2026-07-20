package no.nav.helsemelding.message.msghead.model

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.message.converter.createProvider
import no.nav.helsemelding.message.error.AttachmentMissingError
import no.nav.helsemelding.message.msghead.toOutgoingMessage
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class OutgoingMessageSpec : StringSpec(
    {
        val patientIdent = Personident("24274116206")
        val behandler = createProvider(Uuid.random())

        val employee = Employee(
            firstName = "Ola",
            middleName = "Jens",
            lastName = "Nordmann",
            personident = patientIdent
        )

        val additionalInfo = AdditionalMessageInfo(
            provider = behandler,
            employee = employee,
            createdAt = LocalDateTime.parse("2026-07-06T09:48:44.5727191"),
            dokId = Uuid.parse("769a5524-ca26-4d57-a0f4-d0a1d8f445c9")
        )

        "should return AttachmentMissingError if attachment is null or empty when converting OutgoingDialogMessage(type=FOLLOW_UP_PLAN) to OppfolgingsplanMessage " {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                type = OutgoingDialogMessageType.FOLLOW_UP_PLAN,
                message = "Hei",
                attachment = null
            )

            val error = dialogMessage.toOutgoingMessage(additionalInfo).shouldBeLeft()

            error.shouldBeInstanceOf<AttachmentMissingError>()
            error.message shouldBe "Failed to convert JSON with OutgoingDialogMessageType: FOLLOW_UP_PLAN to FollowUpPlanMessage"
            error.cause shouldBe null
        }

        "should convert OutgoingDialogMessage(type=FOLLOW_UP_PLAN) to OppfolgingsplanMessage if attachment is non-empty. Provided message is ignored" {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                type = OutgoingDialogMessageType.FOLLOW_UP_PLAN,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )

            val followUpPlanMessage = dialogMessage
                .toOutgoingMessage(additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<FollowUpPlanMessage>()

            followUpPlanMessage.id shouldBe dialogMessage.id
            followUpPlanMessage.attachment shouldBe dialogMessage.attachment
            followUpPlanMessage.createdAt shouldBe additionalInfo.createdAt
            followUpPlanMessage.dokId shouldBe additionalInfo.dokId
            followUpPlanMessage.type shouldBe OutgoingDialogMessageType.FOLLOW_UP_PLAN
            followUpPlanMessage.employee shouldBeEqualUsingFields employee
            followUpPlanMessage.provider shouldBeEqualUsingFields behandler
            followUpPlanMessage.message shouldBe "Åpne PDF-vedlegg"
        }

        withData(
            nameFn = { "should convert OutgoingDialogMessage(type=$it) to MemoMessage with provided conversationReference" },
            OutgoingDialogMessageType.RETURN_TO_WORK_NOTIFICATION,
            OutgoingDialogMessageType.MEDICAL_CERTIFICATE_RETURN,
            OutgoingDialogMessageType.MEETING_CANCELLATION,
            OutgoingDialogMessageType.MEETING_EXEMPTION,
            OutgoingDialogMessageType.NAV_FEEDBACK,
            OutgoingDialogMessageType.NAV_MESSAGE,
            OutgoingDialogMessageType.NAV_INFORMATION
        ) {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                type = it,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )

            val memoMessage = dialogMessage
                .toOutgoingMessage(additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<MemoMessage>()

            memoMessage.id shouldBe dialogMessage.id
            memoMessage.attachment shouldBe dialogMessage.attachment
            memoMessage.createdAt shouldBe additionalInfo.createdAt
            memoMessage.dokId shouldBe additionalInfo.dokId
            memoMessage.type shouldBe it
            memoMessage.conversationReference shouldBeEqualUsingFields dialogMessage.conversationReference!!
            memoMessage.employee shouldBeEqualUsingFields employee
            memoMessage.provider shouldBeEqualUsingFields behandler
            memoMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should convert OutgoingDialogMessage(type=$it) without conversationReference to MemoMessage with conversationReference equal to message id" },
            OutgoingDialogMessageType.RETURN_TO_WORK_NOTIFICATION,
            OutgoingDialogMessageType.MEDICAL_CERTIFICATE_RETURN,
            OutgoingDialogMessageType.MEETING_CANCELLATION,
            OutgoingDialogMessageType.MEETING_EXEMPTION,
            OutgoingDialogMessageType.NAV_FEEDBACK,
            OutgoingDialogMessageType.NAV_MESSAGE,
            OutgoingDialogMessageType.NAV_INFORMATION
        ) {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = null,
                type = it,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )

            val memoMessage = dialogMessage
                .toOutgoingMessage(additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<MemoMessage>()

            memoMessage.id shouldBe dialogMessage.id
            memoMessage.attachment shouldBe dialogMessage.attachment
            memoMessage.createdAt shouldBe additionalInfo.createdAt
            memoMessage.dokId shouldBe additionalInfo.dokId
            memoMessage.type shouldBe it
            memoMessage.conversationReference.parentMessageId shouldBe dialogMessage.id
            memoMessage.conversationReference.conversationId shouldBe dialogMessage.id
            memoMessage.employee shouldBeEqualUsingFields employee
            memoMessage.provider shouldBeEqualUsingFields behandler
            memoMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should convert OutgoingDialogMessage(type=$it) to ForesporselMessage with provided conversationReference" },
            OutgoingDialogMessageType.MEETING_INVITATION_2,
            OutgoingDialogMessageType.MEETING_RESCHEDULE_2,
            OutgoingDialogMessageType.MEETING_INVITATION_3,
            OutgoingDialogMessageType.MEETING_RESCHEDULE_3,
            OutgoingDialogMessageType.PATIENT_REQUEST,
            OutgoingDialogMessageType.PATIENT_REQUEST_REMINDER
        ) {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = ConversationReference(
                    parentMessageId = "parent-1",
                    conversationId = "conversation-1"
                ),
                type = it,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )

            val inquiryMessage = dialogMessage
                .toOutgoingMessage(additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<InquiryMessage>()

            inquiryMessage.id shouldBe dialogMessage.id
            inquiryMessage.attachment shouldBe dialogMessage.attachment
            inquiryMessage.createdAt shouldBe additionalInfo.createdAt
            inquiryMessage.dokId shouldBe additionalInfo.dokId
            inquiryMessage.type shouldBe it
            inquiryMessage.conversationReference shouldBeEqualUsingFields dialogMessage.conversationReference!!
            inquiryMessage.employee shouldBeEqualUsingFields employee
            inquiryMessage.provider shouldBeEqualUsingFields behandler
            inquiryMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should convert OutgoingDialogMessage(type=$it) without conversationReference to ForesporselMessage with conversationReference equal to message id" },
            OutgoingDialogMessageType.MEETING_INVITATION_2,
            OutgoingDialogMessageType.MEETING_RESCHEDULE_2,
            OutgoingDialogMessageType.MEETING_INVITATION_3,
            OutgoingDialogMessageType.MEETING_RESCHEDULE_3,
            OutgoingDialogMessageType.PATIENT_REQUEST,
            OutgoingDialogMessageType.PATIENT_REQUEST_REMINDER
        ) {
            val dialogMessage = OutgoingDialogMessage(
                version = 1,
                id = "dialog-1",
                patientIdent = "12345678910",
                providerId = "provider-1",
                conversationReference = null,
                type = it,
                message = "Hei",
                attachment = "QmFzZTY0IGVuY29kZWQgZmlsZQ=="
            )

            val inquiryMessage = dialogMessage
                .toOutgoingMessage(additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<InquiryMessage>()

            inquiryMessage.id shouldBe dialogMessage.id
            inquiryMessage.attachment shouldBe dialogMessage.attachment
            inquiryMessage.createdAt shouldBe additionalInfo.createdAt
            inquiryMessage.dokId shouldBe additionalInfo.dokId
            inquiryMessage.type shouldBe it
            inquiryMessage.conversationReference.parentMessageId shouldBe dialogMessage.id
            inquiryMessage.conversationReference.conversationId shouldBe dialogMessage.id
            inquiryMessage.employee shouldBeEqualUsingFields employee
            inquiryMessage.provider shouldBeEqualUsingFields behandler
            inquiryMessage.message shouldBe dialogMessage.message
        }
    }
)

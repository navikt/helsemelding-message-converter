package no.nav.helsemelding.message.msghead.mapper

import arrow.core.getOrElse
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
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEDICAL_CERTIFICATE_RETURN
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_CANCELLATION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_EXEMPTION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_INVITATION_2
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_INVITATION_3
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_RESCHEDULE_2
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.MEETING_RESCHEDULE_3
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_FEEDBACK
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_INFORMATION
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.NAV_MESSAGE
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.PATIENT_REQUEST
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.PATIENT_REQUEST_REMINDER
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType.RETURN_TO_WORK_NOTIFICATION
import no.nav.helsemelding.message.converter.createProvider
import no.nav.helsemelding.message.error.AttachmentMissingError
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.Personident
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class OutgoingMessageMapperSpec : StringSpec(
    {
        val patientIdent = Personident("24274116206").getOrElse { error ->
            error(error.message)
        }
        val provider = createProvider(Uuid.random())

        val employee = Employee(
            firstName = "Ola",
            middleName = "Jens",
            lastName = "Nordmann",
            personident = patientIdent
        )

        val additionalInfo = AdditionalMessageInfo(
            provider = provider,
            employee = employee,
            createdAt = LocalDateTime.parse("2026-07-06T09:48:44.5727191"),
            docId = Uuid.parse("769a5524-ca26-4d57-a0f4-d0a1d8f445c9")
        )

        "should reject missing follow-up attachment" {
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

            val error = createOutgoingMessage(dialogMessage, additionalInfo).shouldBeLeft()

            error.shouldBeInstanceOf<AttachmentMissingError>()
            error.message shouldBe "Missing follow-up plan attachment"
            error.cause shouldBe null
        }

        "should map follow-up plan" {
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

            val followUpPlanMessage = createOutgoingMessage(dialogMessage, additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<FollowUpPlanMessage>()

            followUpPlanMessage.id shouldBe dialogMessage.id
            followUpPlanMessage.attachment shouldBe dialogMessage.attachment
            followUpPlanMessage.createdAt shouldBe additionalInfo.createdAt
            followUpPlanMessage.docId shouldBe additionalInfo.docId
            followUpPlanMessage.type shouldBe OutgoingDialogMessageType.FOLLOW_UP_PLAN
            followUpPlanMessage.employee shouldBeEqualUsingFields employee
            followUpPlanMessage.provider shouldBeEqualUsingFields provider
            followUpPlanMessage.message shouldBe "Åpne PDF-vedlegg"
        }

        withData(
            nameFn = { "should map memo type=$it with reference" },
            RETURN_TO_WORK_NOTIFICATION,
            MEDICAL_CERTIFICATE_RETURN,
            MEETING_CANCELLATION,
            MEETING_EXEMPTION,
            NAV_FEEDBACK,
            NAV_MESSAGE,
            NAV_INFORMATION
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

            val memoMessage = createOutgoingMessage(dialogMessage, additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<MemoMessage>()

            memoMessage.id shouldBe dialogMessage.id
            memoMessage.attachment shouldBe dialogMessage.attachment
            memoMessage.createdAt shouldBe additionalInfo.createdAt
            memoMessage.docId shouldBe additionalInfo.docId
            memoMessage.type shouldBe it
            memoMessage.conversationReference shouldBeEqualUsingFields dialogMessage.conversationReference!!
            memoMessage.employee shouldBeEqualUsingFields employee
            memoMessage.provider shouldBeEqualUsingFields provider
            memoMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should map memo type=$it without reference" },
            RETURN_TO_WORK_NOTIFICATION,
            MEDICAL_CERTIFICATE_RETURN,
            MEETING_CANCELLATION,
            MEETING_EXEMPTION,
            NAV_FEEDBACK,
            NAV_MESSAGE,
            NAV_INFORMATION
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

            val memoMessage = createOutgoingMessage(dialogMessage, additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<MemoMessage>()

            memoMessage.id shouldBe dialogMessage.id
            memoMessage.attachment shouldBe dialogMessage.attachment
            memoMessage.createdAt shouldBe additionalInfo.createdAt
            memoMessage.docId shouldBe additionalInfo.docId
            memoMessage.type shouldBe it
            memoMessage.conversationReference.parentMessageId shouldBe dialogMessage.id
            memoMessage.conversationReference.conversationId shouldBe dialogMessage.id
            memoMessage.employee shouldBeEqualUsingFields employee
            memoMessage.provider shouldBeEqualUsingFields provider
            memoMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should map inquiry type=$it with reference" },
            MEETING_INVITATION_2,
            MEETING_RESCHEDULE_2,
            MEETING_INVITATION_3,
            MEETING_RESCHEDULE_3,
            PATIENT_REQUEST,
            PATIENT_REQUEST_REMINDER
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

            val inquiryMessage = createOutgoingMessage(dialogMessage, additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<InquiryMessage>()

            inquiryMessage.id shouldBe dialogMessage.id
            inquiryMessage.attachment shouldBe dialogMessage.attachment
            inquiryMessage.createdAt shouldBe additionalInfo.createdAt
            inquiryMessage.docId shouldBe additionalInfo.docId
            inquiryMessage.type shouldBe it
            inquiryMessage.conversationReference shouldBeEqualUsingFields dialogMessage.conversationReference!!
            inquiryMessage.employee shouldBeEqualUsingFields employee
            inquiryMessage.provider shouldBeEqualUsingFields provider
            inquiryMessage.message shouldBe dialogMessage.message
        }

        withData(
            nameFn = { "should map inquiry type=$it without reference" },
            MEETING_INVITATION_2,
            MEETING_RESCHEDULE_2,
            MEETING_INVITATION_3,
            MEETING_RESCHEDULE_3,
            PATIENT_REQUEST,
            PATIENT_REQUEST_REMINDER
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

            val inquiryMessage = createOutgoingMessage(dialogMessage, additionalInfo)
                .shouldBeRight()
                .shouldBeTypeOf<InquiryMessage>()

            inquiryMessage.id shouldBe dialogMessage.id
            inquiryMessage.attachment shouldBe dialogMessage.attachment
            inquiryMessage.createdAt shouldBe additionalInfo.createdAt
            inquiryMessage.docId shouldBe additionalInfo.docId
            inquiryMessage.type shouldBe it
            inquiryMessage.conversationReference.parentMessageId shouldBe dialogMessage.id
            inquiryMessage.conversationReference.conversationId shouldBe dialogMessage.id
            inquiryMessage.employee shouldBeEqualUsingFields employee
            inquiryMessage.provider shouldBeEqualUsingFields provider
            inquiryMessage.message shouldBe dialogMessage.message
        }
    }
)

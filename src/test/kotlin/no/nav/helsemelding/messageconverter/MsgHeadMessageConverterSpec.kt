package no.nav.helsemelding.messageconverter

import arrow.core.Either
import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.helsemelding.messageconverter.error.AdditionalMessageInfoError
import no.nav.helsemelding.messageconverter.error.InvalidJson
import no.nav.helsemelding.messageconverter.error.InvalidXml
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.msghead.XmlSerializer
import no.nav.helsemelding.messageconverter.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.messageconverter.msghead.model.Employee
import no.nav.helsemelding.messageconverter.msghead.model.Personident
import no.nav.helsemelding.messageconverter.msghead.model.provider.OrganisationNumber
import no.nav.helsemelding.messageconverter.msghead.model.provider.Provider
import no.nav.helsemelding.messageconverter.msghead.model.provider.ProviderCategory
import no.nav.helsemelding.messageconverter.msghead.model.provider.ProviderOffice
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

private const val XML_MESSAGE_WITH_ATTACHMENTS_PATH = "src/test/resources/message_with_attachments.xml"
private const val XML_MESSAGE_WITHOUT_ATTACHMENTS_PATH = "src/test/resources/message_without_attachments.xml"
private const val XML_INCOMING_PATIENT_REQUEST_RESPONSE_PATH = "src/test/resources/incoming/PATIENT_REQUEST_RESPONSE.xml"
private const val XML_INCOMING_ACCEPTS_MEETING_INVITATION_PATH = "src/test/resources/incoming/ACCEPTS_MEETING_INVITATION.xml"
private const val XML_INCOMING_SICK_LEAVE_FOLLOW_UP_INQUIRY_PATH = "src/test/resources/incoming/SICK_LEAVE_FOLLOW_UP_INQUIRY.xml"
private const val XML_INCOMING_DECLINES_MEETING_WITH_REASON_PATH = "src/test/resources/incoming/DECLINES_MEETING_WITH_REASON.xml"
private const val XML_INCOMING_PATIENT_INQUIRY_PATH = "src/test/resources/incoming/PATIENT_INQUIRY.xml"
private const val XML_INCOMING_REQUESTS_NEW_MEETING_TIME_PATH = "src/test/resources/incoming/REQUESTS_NEW_MEETING_TIME.xml"
private const val XML_INCOMING_MESSAGE_INVALID_TEMAKODE_PATH = "src/test/resources/incoming/INCOMING_MESSAGE_INVALID_TEMAKODE.xml"

class MsgHeadMessageConverterSpec : StringSpec(
    {
        val converter = msgHeadMessageConverter()
        val serializer = XmlSerializer()

        "should convert MsgHead XML to DialogMessage JSON" {
            val messageXml = Files.readString(Paths.get(XML_INCOMING_PATIENT_REQUEST_RESPONSE_PATH))

            val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe
                Json.parseToJsonElement(
                    """
                    {
                      "version": 1,
                      "id": "f4afe2d3-2d00-40b3-95d0-0b537bf43637",
                      "type": "PATIENT_REQUEST_RESPONSE",
                      "receivedAt": "2025-10-10T10:02:36.257096900",
                      "patientIdent": "26076725771",
                      "sender": {
                        "providerId": "959409587",
                        "signingProviderId": "1111"
                      },
                      "conversationReference": {
                        "parentMessageId": "72c7b6a8-3abf-4c1b-9780-eb6eda94447a",
                        "conversationId": "72c7b6a8-3abf-4c1b-9780-eb6eda94447a"
                      },
                      "message": "",
                      "numberOfAttachments": 0
                    }
                    """.trimIndent()
                )
        }

        listOf(
            XML_INCOMING_PATIENT_REQUEST_RESPONSE_PATH to "PATIENT_REQUEST_RESPONSE",
            XML_INCOMING_ACCEPTS_MEETING_INVITATION_PATH to "ACCEPTS_MEETING_INVITATION",
            XML_INCOMING_SICK_LEAVE_FOLLOW_UP_INQUIRY_PATH to "SICK_LEAVE_FOLLOW_UP_INQUIRY",
            XML_INCOMING_DECLINES_MEETING_WITH_REASON_PATH to "DECLINES_MEETING_WITH_REASON",
            XML_INCOMING_PATIENT_INQUIRY_PATH to "PATIENT_INQUIRY",
            XML_INCOMING_REQUESTS_NEW_MEETING_TIME_PATH to "REQUESTS_NEW_MEETING_TIME"
        ).forEach { (path, expectedType) ->
            "should resolve type $expectedType from incoming XML" {
                val messageXml = Files.readString(Paths.get(path))

                val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

                Json.parseToJsonElement(json).jsonObject["type"]?.jsonPrimitive?.content shouldBe expectedType
            }
        }

        "should return MappingError when TemaKodet attribute combination is unknown" {
            val messageXml = Files.readString(Paths.get(XML_INCOMING_MESSAGE_INVALID_TEMAKODE_PATH))

            val error = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeLeft()

            error.shouldBeInstanceOf<MappingError>()
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

        "should return AdditionalMessageInfoError when additionalMessageProvider returns error" {
            val providerId = Uuid.parse("75837362-2d8c-4f50-9ba5-961999bf1acc")
            val msgId = Uuid.random()
            val json = Json.parseToJsonElement(
                """
                    {
                        "version": 1,
                        "id": "$msgId",
                        "patientIdent": "12345678910",
                        "providerId": "$providerId",
                        "conversationReference": {
                            "parentMessageId": "uuid3",
                            "conversationId": "uuid4"
                        },
                        "type": "MEETING_INVITATION_2",
                        "message": "Hei",
                        "attachment": "attachment"
                    }
                """.trimIndent()
            ).toString()

            val error = converter.outgoingDialogMessageJsonToXml(json).shouldBeLeft()

            error.shouldBeInstanceOf<AdditionalMessageInfoError>()
            error.message shouldBe "Error when fetching additional message info"
        }

        "should require provider for outgoing conversion" {
            val providerId = Uuid.parse("75837362-2d8c-4f50-9ba5-961999bf1acc")
            val msgId = Uuid.random()
            val json = Json.parseToJsonElement(
                """
                    {
                        "version": 1,
                        "id": "$msgId",
                        "patientIdent": "12345678910",
                        "providerId": "$providerId",
                        "conversationReference": {
                            "parentMessageId": "uuid3",
                            "conversationId": "uuid4"
                        },
                        "type": "MEETING_INVITATION_2",
                        "message": "Hei",
                        "attachment": "attachment"
                    }
                """.trimIndent()
            ).toString()

            val error = MsgHeadMessageConverter().outgoingDialogMessageJsonToXml(json).shouldBeLeft()

            error.shouldBeInstanceOf<AdditionalMessageInfoError>()
            error.message shouldBe "AdditionalMessageInfoProvider is required for outgoing conversion"
        }

        "should convert OutgoingDialogMessage JSON to MsgHead XML" {
            val providerId = Uuid.parse("75837362-2d8c-4f50-9ba5-961999bf1acc")
            val patientIdent = Personident("12345678910").getOrElse { error ->
                error(error.message)
            }
            val msgId = Uuid.random()
            val json = Json.parseToJsonElement(
                """
                    {
                        "version": 1,
                        "id": "$msgId",
                        "patientIdent": "${patientIdent.value}",
                        "providerId": "$providerId",
                        "conversationReference": {
                            "parentMessageId": "uuid3",
                            "conversationId": "uuid4"
                        },
                        "type": "MEETING_INVITATION_2",
                        "message": "Hei",
                        "attachment": "attachment"
                    }
                """.trimIndent()
            ).toString()

            val provider = createProvider(providerId)
            val employee = Employee(
                firstName = "Ola",
                middleName = "Jens",
                lastName = "Nordmann",
                personident = patientIdent
            )
            val additionalMessageInfoProvider = FakeAdditionalMessageInfoProvider()
            additionalMessageInfoProvider.givenAdditionalMessageInfo(
                msgId = msgId,
                either = Either.Right(AdditionalMessageInfo(provider, employee))
            )

            val converter = msgHeadMessageConverter(additionalMessageInfoProvider)
            converter.outgoingDialogMessageJsonToXml(json).shouldBeRight()
        }

        "should extract attachments from MsgHead XML" {
            val messageXml = Files.readString(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH))

            val attachments = converter.extractAttachments(messageXml).shouldBeRight()

            attachments.size shouldBe 3
            attachments.map { it.description } shouldContainExactly listOf(
                "Testvedlegg 1",
                "Testvedlegg 2",
                "Testvedlegg 3"
            )
            attachments.map { it.contentType }.distinct() shouldContainExactly listOf("application/pdf")
        }

        "should split attachments from MsgHead XML" {
            val messageXml = Files.readString(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH))

            val splitMessage = converter.splitAttachments(messageXml).shouldBeRight()
            val msgHead = serializer.deserialize(splitMessage.messageWithoutAttachmentsXml).shouldBeRight()

            splitMessage.attachments.size shouldBe 3
            splitMessage.attachments.map { it.description } shouldContainExactly listOf(
                "Testvedlegg 1",
                "Testvedlegg 2",
                "Testvedlegg 3"
            )
            msgHead.document.size shouldBe 1
            splitMessage.messageWithoutAttachmentsXml shouldContain "MsgHead"
        }

        "should return empty attachment list when MsgHead XML has no attachments" {
            val messageXml = Files.readString(Paths.get(XML_MESSAGE_WITHOUT_ATTACHMENTS_PATH))

            converter.extractAttachments(messageXml).shouldBeRight() shouldBe emptyList()
        }

        "should remove attachments from MsgHead XML" {
            val messageXml = Files.readString(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH))

            val xmlWithoutAttachments = converter.removeAttachments(messageXml).shouldBeRight()
            val msgHead = serializer.deserialize(xmlWithoutAttachments).shouldBeRight()

            msgHead.document.size shouldBe 1
            xmlWithoutAttachments shouldContain "MsgHead"
        }
    }
)

private fun msgHeadMessageConverter(
    additionalMessageInfoProvider: AdditionalMessageInfoProvider = FakeAdditionalMessageInfoProvider()
): MsgHeadMessageConverter = MsgHeadMessageConverter(
    additionalMessageInfoProvider = additionalMessageInfoProvider
)

fun createProvider(
    providerReference: Uuid,
    dialogMessageEnabled: Boolean = true,
    dialogMessageEnabledLocked: Boolean = false,
    officeName: String? = null,
    personident: Personident = Personident("13326920147").getOrElse { error ->
        error(error.message)
    },
    herId: Int? = 654321,
    hprId: Int = 7654321,
    category: ProviderCategory = ProviderCategory.DOCTOR,
    organisationNumber: String? = "987654321"
) = Provider(
    providerReference = providerReference,
    office = ProviderOffice(
        herId = 54321,
        name = officeName,
        address = "Storgata 15",
        postalCode = "0158",
        city = "Oslo",
        organisationNumber = organisationNumber?.let { value ->
            OrganisationNumber(value).getOrElse { error ->
                error(error.message)
            }
        },
        dialogMessageEnabled = dialogMessageEnabled,
        dialogMessageEnabledLocked = dialogMessageEnabledLocked,
        system = null,
        receivedAt = OffsetDateTime.now()
    ),
    nationalIdentityNumber = personident,
    firstName = "Kari",
    middleName = "Anne",
    lastName = "Hansen",
    herId = herId,
    hprId = hprId,
    phoneNumber = null,
    category = category,
    receivedAt = OffsetDateTime.now(),
    suspended = false
)

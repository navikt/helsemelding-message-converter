package no.nav.helsemelding.messageconverter

import arrow.core.Either
import arrow.core.getOrElse
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
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
private const val XML_INCOMING_MESSAGE_INVALID_TEMAKODE_PATH = "src/test/resources/incoming/invalid/INCOMING_MESSAGE_INVALID_TEMAKODE.xml"

private fun incomingXmlPath(name: String) = "src/test/resources/incoming/$name.xml"
private fun incomingJsonPath(name: String) = "src/test/resources/incoming/$name.json"

class MsgHeadMessageConverterSpec : StringSpec(
    {
        val converter = msgHeadMessageConverter()
        val serializer = XmlSerializer()

        withData(
            nameFn = { "should convert $it XML to DialogMessage JSON" },
            listOf(
                "PATIENT_REQUEST_RESPONSE",
                "ACCEPTS_MEETING_INVITATION",
                "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                "DECLINES_MEETING_WITH_REASON",
                "PATIENT_INQUIRY",
                "REQUESTS_NEW_MEETING_TIME"
            )
        ) { name ->
            val messageXml = Files.readString(Paths.get(incomingXmlPath(name)))
            val expectedJson = Files.readString(Paths.get(incomingJsonPath(name)))

            val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe Json.parseToJsonElement(expectedJson)
        }

        withData(
            nameFn = { "should resolve type $it from incoming XML" },
            listOf(
                "PATIENT_REQUEST_RESPONSE",
                "ACCEPTS_MEETING_INVITATION",
                "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                "DECLINES_MEETING_WITH_REASON",
                "PATIENT_INQUIRY",
                "REQUESTS_NEW_MEETING_TIME"
            )
        ) { name ->
            val messageXml = Files.readString(Paths.get(incomingXmlPath(name)))

            val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

            Json.parseToJsonElement(json).jsonObject["type"]?.jsonPrimitive?.content shouldBe name
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

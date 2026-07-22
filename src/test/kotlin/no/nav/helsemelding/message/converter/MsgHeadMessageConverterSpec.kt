package no.nav.helsemelding.message.converter

import arrow.core.Either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import no.nav.helsemelding.message.error.AdditionalMessageInfoError
import no.nav.helsemelding.message.error.InvalidJson
import no.nav.helsemelding.message.error.InvalidXml
import no.nav.helsemelding.message.msghead.XmlSerializer
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.Personident
import no.nav.helsemelding.message.msghead.model.provider.OrganisationNumber
import no.nav.helsemelding.message.msghead.model.provider.Provider
import no.nav.helsemelding.message.msghead.model.provider.ProviderCategori
import no.nav.helsemelding.message.msghead.model.provider.ProviderOffice
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

private const val XML_MESSAGE_WITH_ATTACHMENTS_PATH = "src/test/resources/message_with_attachments.xml"
private const val XML_MESSAGE_WITHOUT_ATTACHMENTS_PATH = "src/test/resources/message_without_attachments.xml"

class MsgHeadMessageConverterSpec : StringSpec(
    {
        val converter = msgHeadMessageConverter()
        val serializer = XmlSerializer()

        "should convert MsgHead XML to DialogMessage JSON" {
            val messageXml = Files.readString(Paths.get(XML_MESSAGE_WITH_ATTACHMENTS_PATH))

            val json = converter.incomingDialogMessageXmlToJson(messageXml).shouldBeRight()

            Json.parseToJsonElement(json) shouldBe
                Json.parseToJsonElement(
                    """
                    {
                      "version": 1,
                      "id": "df978545-189c-4ad2-8479-d5271d69e0b6",
                      "type": "SICK_LEAVE_FOLLOW_UP_INQUIRY",
                      "receivedAt": "2026-05-29T13:13:28.967022541",
                      "patientIdent": "31777207884",
                      "sender": {
                        "providerId": "8142520",
                        "signingProviderId": "8142520"
                      },
                      "conversationReference": null,
                      "message": "Har du forslag til tilrettelegging på arbeidsplassen for den sykmeldte?",
                      "numberOfAttachments": 3
                    }
                    """.trimIndent()
                )
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

        "should convert OutgoingDialogMessage JSON to MsgHead XML" {
            val providerId = Uuid.parse("75837362-2d8c-4f50-9ba5-961999bf1acc")
            val patientIdent = Personident("12345678910")
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
    behandlerRef: Uuid,
    dialogmeldingEnabled: Boolean = true,
    dialogmeldingEnabledLocked: Boolean = false,
    kontornavn: String? = null,
    personident: Personident = Personident("13326920147"),
    herId: Int? = 654321,
    hprId: Int = 7654321,
    kategori: ProviderCategori = ProviderCategori.LEGE,
    orgnummer: String? = "987654321"
) = Provider(
    behandlerRef = behandlerRef,
    kontor = ProviderOffice(
        herId = 54321,
        navn = kontornavn,
        adresse = "Storgata 15",
        postnummer = "0158",
        poststed = "Oslo",
        orgnummer = orgnummer?.let { OrganisationNumber(it) },
        dialogmeldingEnabled = dialogmeldingEnabled,
        dialogmeldingEnabledLocked = dialogmeldingEnabledLocked,
        system = null,
        mottatt = OffsetDateTime.now()
    ),
    personident = personident,
    fornavn = "Kari",
    mellomnavn = "Anne",
    etternavn = "Hansen",
    herId = herId,
    hprId = hprId,
    telefon = null,
    kategori = kategori,
    mottatt = OffsetDateTime.now(),
    suspendert = false
)

package no.nav.helsemelding.message.converter

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.coroutines.runBlocking
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.message.attachment.AttachmentHandler
import no.nav.helsemelding.message.client.pdl.PdlClient
import no.nav.helsemelding.message.client.providerregistry.ProviderRegistryClient
import no.nav.helsemelding.message.error.AttachmentError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.MappingError
import no.nav.helsemelding.message.json.IncomingDialogMessageSerializer
import no.nav.helsemelding.message.json.OutgoingDialogMessageSerializer
import no.nav.helsemelding.message.model.Attachment
import no.nav.helsemelding.message.model.SplitMessage
import no.nav.helsemelding.message.msghead.MsgHeadDialogMessageMapper
import no.nav.helsemelding.message.msghead.XmlSerializer
import no.nav.helsemelding.message.msghead.extractAttachmentDocuments
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.Personident
import no.nav.helsemelding.message.msghead.removeAttachmentDocuments
import no.nav.helsemelding.message.msghead.toAttachment
import no.nav.helsemelding.message.msghead.toOutgoingMessage
import kotlin.uuid.Uuid

class MsgHeadMessageConverter(
    private val xmlSerializer: XmlSerializer = XmlSerializer(),
    private val incomingDialogMessageSerializer: IncomingDialogMessageSerializer = IncomingDialogMessageSerializer(),
    private val outgoingDialogMessageSerializer: OutgoingDialogMessageSerializer = OutgoingDialogMessageSerializer(),
    private val mapper: MsgHeadDialogMessageMapper = MsgHeadDialogMessageMapper(),
    private val pdlClient: PdlClient,
    private val providerRegistryClient: ProviderRegistryClient
) : MessageConverter, AttachmentHandler {
    override fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(xml).bind()
            val dialogMessage = mapper.toIncomingDialogMessage(msgHead).bind()

            incomingDialogMessageSerializer.serialize(dialogMessage).bind()
        }

    override fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String> =
        either {
            val dialogMessage = outgoingDialogMessageSerializer.deserialize(json).bind()
            val additionalMessageInfo = getAdditionalMessageInfo(dialogMessage).bind()
            val outgoingMessage = dialogMessage.toOutgoingMessage(additionalMessageInfo).bind()
            val msgHead = mapper.toMsgHead(outgoingMessage).bind()

            xmlSerializer.serialize(msgHead).bind()
        }

    private fun getAdditionalMessageInfo(dialogMessage: OutgoingDialogMessage): Either<ConversionError, AdditionalMessageInfo> =
        either {
            val behandler = runBlocking {
                providerRegistryClient.getProvider(Uuid.parse(dialogMessage.providerId))
                    .mapLeft { MappingError(it.message) }
                    .bind()
            }

            val patientIdent = Personident(dialogMessage.patientIdent)
            val employee = runBlocking {
                pdlClient.getPersonName(patientIdent)
                    .mapLeft { MappingError(it.message) }
                    .map {
                        Employee(
                            personident = patientIdent,
                            firstName = it.fornavn,
                            middleName = it.mellomnavn,
                            lastName = it.etternavn
                        )
                    }
                    .bind()
            }

            return Either.Right(AdditionalMessageInfo(behandler, employee))
        }

    override fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage> =
        either {
            SplitMessage(
                messageWithoutAttachmentsXml = removeAttachments(msgHeadXml).bind(),
                attachments = extractAttachments(msgHeadXml).bind()
            )
        }

    override fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>> =
        either {
            val msgHead = xmlSerializer.deserialize(msgHeadXml).bind()
            val documents = Either.catch { msgHead.extractAttachmentDocuments() }
                .mapLeft { AttachmentError("Could not extract attachment documents from MsgHead", it) }
                .bind()
            val attachments = mutableListOf<Attachment>()

            for (document in documents) {
                attachments += document.toAttachment().bind()
            }

            attachments
        }

    override fun removeAttachments(msgHeadXml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(msgHeadXml).bind()

            Either.catch { msgHead.removeAttachmentDocuments() }
                .mapLeft { AttachmentError("Could not remove attachment documents from MsgHead", it) }
                .bind()

            xmlSerializer.serialize(msgHead).bind()
        }
}

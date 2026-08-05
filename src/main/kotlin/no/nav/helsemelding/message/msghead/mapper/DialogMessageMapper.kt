package no.nav.helsemelding.message.msghead.mapper

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.base64container.Base64Container
import no.nav.helse.dialogmelding.CV
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.dialogmelding.XMLForesporsel
import no.nav.helse.dialogmelding.XMLNotat
import no.nav.helse.dialogmelding.XMLPerson
import no.nav.helse.dialogmelding.XMLRollerRelatertNotat
import no.nav.helse.msgHead.XMLAddress
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLConversationRef
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLPatient
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helse.msgHead.XMLTS
import no.nav.helsemelding.jsonschema.core.model.ConversationReference
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessageType
import no.nav.helsemelding.jsonschema.core.model.OutgoingType.DIALOG_NOTE
import no.nav.helsemelding.jsonschema.core.model.OutgoingType.DIALOG_REQUEST
import no.nav.helsemelding.message.error.AttachmentError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.OutgoingMessage
import no.nav.helsemelding.message.msghead.model.Personident
import no.nav.helsemelding.message.msghead.model.isDNR
import no.nav.helsemelding.message.msghead.model.provider.Provider
import no.nav.helsemelding.message.msghead.model.provider.ProviderOffice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

private const val CODE_SYSTEM_BASE = "2.16.578.1.12.4.1.1."

internal fun createBaseDialogMessage(message: OutgoingMessage): XMLMsgInfo {
    return XMLMsgInfo().apply {
        type = createType(message.type)
        miGversion = "v1.2 2006-05-24"
        genDate = message.createdAt
        msgId = message.id
        ack = XMLCS().apply {
            dn = "Ja"
            v = "J"
        }
        sender = createSender()
        patient = createPatient(message.employee)
    }
}

internal fun createConversationRef(conversationReference: ConversationReference): XMLConversationRef {
    return XMLConversationRef().apply {
        refToConversation = conversationReference.conversationId
        refToParent = conversationReference.parentMessageId
    }
}

internal fun createType(outgoingDialogMessageType: OutgoingDialogMessageType): XMLCS {
    return when (outgoingDialogMessageType.messageType) {
        DIALOG_REQUEST -> XMLCS().apply {
            dn = "Forespørsel"
            v = "DIALOG_FORESPORSEL"
        }

        DIALOG_NOTE -> XMLCS().apply {
            dn = "Notat"
            v = "DIALOG_NOTAT"
        }
    }
}

internal fun createAttachmentDocument(
    attachmentBase64: String,
    createdAt: LocalDateTime
): Either<ConversionError, XMLDocument> = either {
    val attachment = decodeAttachment(attachmentBase64).bind()

    XMLDocument().apply {
        documentConnection = XMLCS().apply {
            dn = "Vedlegg"
            v = "V"
        }
        refDoc = XMLRefDoc().apply {
            issueDate = XMLTS().apply {
                v = createdAt.format(DateTimeFormatter.ISO_DATE)
            }
            msgType = XMLCS().apply {
                dn = "Vedlegg"
                v = "A"
            }
            mimeType = "application/pdf"
            content = XMLRefDoc.Content().apply {
                any.add(Base64Container().apply { value = attachment })
            }
        }
    }
}

internal fun createSender(): XMLSender {
    return XMLSender().apply {
        organisation = XMLOrganisation().apply {
            organisationName = "NAV"
            ident.add(
                XMLIdent().apply {
                    id = "889640782"
                    typeId = createOrganisationNumberType()
                }
            )
            ident.add(
                XMLIdent().apply {
                    id = "8142519"
                    typeId = createHerId()
                }
            )
        }
    }
}

internal fun createOrganisationNumberType(): XMLCV {
    return XMLCV().apply {
        dn = "Organisasjonsnummeret i Enhetsregisteret"
        s = "2.16.578.1.12.4.1.1.9051"
        v = "ENH"
    }
}

internal fun createHerId(): XMLCV {
    return XMLCV().apply {
        dn = "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
        s = "2.16.578.1.12.4.1.1.9051"
        v = "HER"
    }
}

internal fun createReceiver(
    provider: Provider,
    roleToPatient: XMLHealthcareProfessional.() -> Unit = {}
): XMLReceiver = XMLReceiver().apply {
    organisation = createReceiverOrganisation(provider, roleToPatient)
}

private fun decodeAttachment(attachmentBase64: String): Either<ConversionError, ByteArray> =
    Either.catch {
        Base64.getDecoder().decode(attachmentBase64)
    }
        .mapLeft { AttachmentError("Could not decode base64 attachment", it) }

private fun createReceiverOrganisation(
    provider: Provider,
    roleToPatient: XMLHealthcareProfessional.() -> Unit
): XMLOrganisation = XMLOrganisation().apply {
    organisationName = provider.office.name
    provider.office.organisationNumber?.let {
        ident.add(createOrganisationNumberIdent(it.value))
    }
    ident.add(createHerIdIdent(provider.office.herId.toString()))
    address = createAddress(provider.office)
    healthcareProfessional = createHealthcareProfessional(provider, roleToPatient)
}

private fun createOrganisationNumberIdent(organisationNumber: String): XMLIdent =
    XMLIdent().apply {
        id = organisationNumber
        typeId = createOrganisationNumberType()
    }

private fun createHerIdIdent(herId: String): XMLIdent =
    XMLIdent().apply {
        id = herId
        typeId = createHerId()
    }

private fun createAddress(office: ProviderOffice): XMLAddress =
    XMLAddress().apply {
        type = XMLCS().apply {
            dn = "Besøksadresse"
            v = "RES"
        }
        streetAdr = office.address
        postalCode = office.postalCode
        city = office.city
    }

private fun createHealthcareProfessional(
    provider: Provider,
    roleToPatient: XMLHealthcareProfessional.() -> Unit
): XMLHealthcareProfessional = XMLHealthcareProfessional().apply {
    roleToPatient()
    familyName = provider.lastName
    middleName = provider.middleName
    givenName = provider.firstName
    addProviderIdentifiers(provider)
}

private fun XMLHealthcareProfessional.addProviderIdentifiers(provider: Provider) {
    provider.nationalIdentityNumber?.let {
        ident.add(createXMLIdentForPersonIdent(it))
    }
    provider.hprId?.let {
        ident.add(createHprIdent(it))
    }
    provider.herId?.let {
        ident.add(createHealthcareProfessionalHerIdIdent(it))
    }
}

private fun createHprIdent(hprId: Int): XMLIdent = XMLIdent().apply {
    id = hprId.toString()
    typeId = XMLCV().apply {
        dn = "HPR-nummer"
        s = "2.16.578.1.12.4.1.1.8116"
        v = "HPR"
    }
}

private fun createHealthcareProfessionalHerIdIdent(herId: Int): XMLIdent = XMLIdent().apply {
    id = herId.toString()
    typeId = XMLCV().apply {
        dn = "Identifikator fra Helsetjenesteenhetsregisteret"
        s = "2.16.578.1.12.4.1.1.8116"
        v = "HER"
    }
}

internal fun XMLHealthcareProfessional.roleToPatient() {
    roleToPatient = XMLCV().apply {
        v = "6"
        s = "2.16.578.1.12.4.1.1.9034"
        dn = "Fastlege"
    }
}

internal fun createXMLIdentForPersonIdent(personIdent: Personident): XMLIdent {
    val isPersonIdentDNumber = personIdent.isDNR()
    return XMLIdent().apply {
        id = personIdent.value
        typeId = XMLCV().apply {
            dn = if (isPersonIdentDNumber) "D-nummer" else "Fødselsnummer"
            s = "2.16.578.1.12.4.1.1.8116"
            v = if (isPersonIdentDNumber) "DNR" else "FNR"
        }
    }
}

internal fun createPatient(employee: Employee): XMLPatient = XMLPatient().apply {
    familyName = employee.lastName
    middleName = employee.middleName
    givenName = employee.firstName
    ident.add(createXMLIdentForPersonIdent(employee.personident))
}

internal fun createDialogMessageDocument(
    outgoingMessage: OutgoingMessage,
    dialogMessage: XMLDialogmelding
): XMLDocument = XMLDocument().apply {
    documentConnection = XMLCS().apply {
        dn = "Hoveddokument"
        v = "H"
    }
    refDoc = XMLRefDoc().apply {
        issueDate = XMLTS().apply {
            v = outgoingMessage.createdAt.format(DateTimeFormatter.ISO_DATE)
        }
        msgType = XMLCS().apply {
            dn = "XML-instans"
            v = "XML"
        }
        mimeType = "text/xml"
        content = XMLRefDoc.Content().apply {
            any.add(dialogMessage)
        }
    }
}

internal fun createInquiryDialogMessage(inquiryMessage: InquiryMessage): XMLDialogmelding =
    XMLDialogmelding().apply {
        foresporsel.add(
            XMLForesporsel().apply {
                typeForesp = CV().apply {
                    dn = inquiryMessage.type.application
                    s = "$CODE_SYSTEM_BASE${inquiryMessage.type.codeSystem}"
                    v = inquiryMessage.type.code.toString()
                }
                sporsmal = inquiryMessage.message
                dokIdForesp = inquiryMessage.docId.toString()
            }
        )
    }

internal fun createMemoDialogMessage(memoMessage: MemoMessage): XMLDialogmelding =
    XMLDialogmelding().apply {
        notat.add(
            XMLNotat().apply {
                temaKodet = CV().apply {
                    dn = memoMessage.type.application
                    s = "$CODE_SYSTEM_BASE${memoMessage.type.codeSystem}"
                    v = memoMessage.type.code.toString()
                }
                tekstNotatInnhold = memoMessage.message
                dokIdNotat = memoMessage.docId.toString()
            }
        )
    }

internal fun createFollowUpPlanDialogMessage(followUpPlanMessage: FollowUpPlanMessage): XMLDialogmelding {
    return XMLDialogmelding().apply {
        notat.add(
            XMLNotat().apply {
                temaKodet = CV().apply {
                    dn = followUpPlanMessage.type.application
                    s = "$CODE_SYSTEM_BASE${followUpPlanMessage.type.codeSystem}"
                    v = followUpPlanMessage.type.code.toString()
                }
                tekstNotatInnhold = followUpPlanMessage.message
                dokIdNotat = followUpPlanMessage.docId.toString()
                rollerRelatertNotat.add(
                    XMLRollerRelatertNotat().apply {
                        rolleNotat = CV().apply {
                            s = "2.16.578.1.12.4.1.1.9057"
                            v = "1"
                        }
                        person = XMLPerson()
                    }
                )
            }
        )
    }
}

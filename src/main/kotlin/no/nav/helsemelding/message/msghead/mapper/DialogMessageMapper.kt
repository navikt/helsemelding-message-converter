package no.nav.helsemelding.message.msghead.mapper

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
import no.nav.helsemelding.jsonschema.core.model.OutgoingType.DIALOG_FORESPORSEL
import no.nav.helsemelding.jsonschema.core.model.OutgoingType.DIALOG_NOTAT
import no.nav.helsemelding.message.client.providerregistry.model.Provider
import no.nav.helsemelding.message.msghead.model.Employee
import no.nav.helsemelding.message.msghead.model.FollowUpPlanMessage
import no.nav.helsemelding.message.msghead.model.InquiryMessage
import no.nav.helsemelding.message.msghead.model.MemoMessage
import no.nav.helsemelding.message.msghead.model.OutgoingMessage
import no.nav.helsemelding.message.msghead.model.Personident
import no.nav.helsemelding.message.msghead.model.isDNR
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val KODEVERK_BASE = "2.16.578.1.12.4.1.1."

fun createBaseDialogMessage(message: OutgoingMessage): XMLMsgInfo {
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

fun createConversationRef(conversationReference: ConversationReference): XMLConversationRef {
    return XMLConversationRef().apply {
        refToConversation = conversationReference.conversationId
        refToParent = conversationReference.parentMessageId
    }
}

fun createType(outgoingDialogMessageType: OutgoingDialogMessageType): XMLCS {
    return when (outgoingDialogMessageType.messageType) {
        DIALOG_FORESPORSEL -> XMLCS().apply {
            dn = "Forespørsel"
            v = outgoingDialogMessageType.messageType.name
        }

        DIALOG_NOTAT -> XMLCS().apply {
            dn = "Notat"
            v = outgoingDialogMessageType.messageType.name
        }
    }
}

fun createAttachmentDocument(attachment: ByteArray, createdAt: LocalDateTime): XMLDocument {
    return XMLDocument().apply {
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

fun createSender(): XMLSender {
    return XMLSender().apply {
        organisation = XMLOrganisation().apply {
            organisationName = "NAV"
            ident.add(
                XMLIdent().apply {
                    id = "889640782"
                    typeId = createEnhetsregister()
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

fun createEnhetsregister(): XMLCV {
    return XMLCV().apply {
        dn = "Organisasjonsnummeret i Enhetsregisteret"
        s = "2.16.578.1.12.4.1.1.9051"
        v = "ENH"
    }
}

fun createHerId(): XMLCV {
    return XMLCV().apply {
        dn = "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
        s = "2.16.578.1.12.4.1.1.9051"
        v = "HER"
    }
}

fun createReceiver(
    provider: Provider,
    roleToPatient: XMLHealthcareProfessional.() -> Unit = {}
): XMLReceiver {
    return XMLReceiver().apply {
        organisation = XMLOrganisation().apply {
            organisationName = provider.kontor.navn
            if (provider.kontor.orgnummer != null) {
                ident.add(
                    XMLIdent().apply {
                        id = provider.kontor.orgnummer.value
                        typeId = createEnhetsregister()
                    }
                )
            }
            ident.add(
                XMLIdent().apply {
                    id = provider.kontor.herId.toString()
                    typeId = createHerId()
                }
            )
            address = XMLAddress().apply {
                type = XMLCS().apply {
                    dn = "Besøksadresse"
                    v = "RES"
                }
                streetAdr = provider.kontor.adresse
                postalCode = provider.kontor.postnummer
                city = provider.kontor.poststed
            }
            healthcareProfessional = XMLHealthcareProfessional().apply {
                roleToPatient()
                familyName = provider.etternavn
                middleName = provider.mellomnavn
                givenName = provider.fornavn
                provider.personident?.let {
                    ident.add(createXMLIdentForPersonident(it))
                }
                if (provider.hprId != null) {
                    ident.add(
                        XMLIdent().apply {
                            id = provider.hprId.toString()
                            typeId = XMLCV().apply {
                                dn = "HPR-nummer"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HPR"
                            }
                        }
                    )
                }
                if (provider.herId != null) {
                    ident.add(
                        XMLIdent().apply {
                            id = provider.herId.toString()
                            typeId = XMLCV().apply {
                                dn = "Identifikator fra Helsetjenesteenhetsregisteret"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HER"
                            }
                        }
                    )
                }
            }
        }
    }
}

fun XMLHealthcareProfessional.roleToPatient() {
    roleToPatient = XMLCV().apply {
        v = "6"
        s = "2.16.578.1.12.4.1.1.9034"
        dn = "Fastlege"
    }
}

fun createXMLIdentForPersonident(personident: Personident): XMLIdent {
    val isPersonidentDNR = personident.isDNR()
    return XMLIdent().apply {
        id = personident.value
        typeId = XMLCV().apply {
            dn = if (isPersonidentDNR) "D-nummer" else "Fødselsnummer"
            s = "2.16.578.1.12.4.1.1.8116"
            v = if (isPersonidentDNR) "DNR" else "FNR"
        }
    }
}

fun createPatient(employee: Employee): XMLPatient {
    return XMLPatient().apply {
        familyName = employee.lastName
        middleName = employee.middleName
        givenName = employee.firstName
        ident.add(createXMLIdentForPersonident(employee.personident))
    }
}

fun createDialogMessageDocument(
    outgoingMessage: OutgoingMessage,
    dialogmelding: XMLDialogmelding
): XMLDocument {
    return XMLDocument().apply {
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
                any.add(dialogmelding)
            }
        }
    }
}

fun inquiry(inquiryMessage: InquiryMessage): XMLDialogmelding {
    return XMLDialogmelding().apply {
        foresporsel.add(
            XMLForesporsel().apply {
                typeForesp = CV().apply {
                    dn = inquiryMessage.type.application
                    s = "$KODEVERK_BASE${inquiryMessage.type.codeSystem}"
                    v = inquiryMessage.type.code.toString()
                }
                sporsmal = inquiryMessage.message
                dokIdForesp = inquiryMessage.dokId.toString()
            }
        )
    }
}

fun memo(memoMessage: MemoMessage): XMLDialogmelding {
    return XMLDialogmelding().apply {
        notat.add(
            XMLNotat().apply {
                temaKodet = CV().apply {
                    dn = memoMessage.type.application
                    s = "$KODEVERK_BASE${memoMessage.type.codeSystem}"
                    v = memoMessage.type.code.toString()
                }
                tekstNotatInnhold = memoMessage.message
                dokIdNotat = memoMessage.dokId.toString()
            }
        )
    }
}

fun followUpPlan(followUpPlanMessage: FollowUpPlanMessage): XMLDialogmelding {
    return XMLDialogmelding().apply {
        notat.add(
            XMLNotat().apply {
                temaKodet = CV().apply {
                    dn = followUpPlanMessage.type.application
                    s = "$KODEVERK_BASE${followUpPlanMessage.type.codeSystem}"
                    v = followUpPlanMessage.type.code.toString()
                }
                tekstNotatInnhold = followUpPlanMessage.message
                dokIdNotat = followUpPlanMessage.dokId.toString()
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

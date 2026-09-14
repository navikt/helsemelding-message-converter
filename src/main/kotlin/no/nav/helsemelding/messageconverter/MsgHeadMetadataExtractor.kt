package no.nav.helsemelding.messageconverter

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.MappingError
import no.nav.helsemelding.messageconverter.model.MessageMetadata

class MsgHeadMetadataExtractor {
    fun extract(msgHead: XMLMsgHead): Either<ConversionError, MessageMetadata> = either {
        val info = ensureNotNull(msgHead.msgInfo) { missing("msgInfo") }
        val type = ensureNotNull(info.type?.v?.takeIf { it.isNotBlank() }) { missing("msgInfo.type.v") }
        val senderHerId = herId(info.sender?.organisation, "msgInfo.sender").bind()
        val receiverHerId = herId(info.receiver?.organisation, "msgInfo.receiver").bind()
        val otherReceivers = info.otherReceiver.mapIndexed { index, receiver ->
            herId(
                receiver.organisation,
                "msgInfo.otherReceiver[$index]",
                receiver.healthcareProfessional
            )
                .bind()
        }
        MessageMetadata(
            senderHerId = senderHerId,
            receiverHerIds = (listOf(receiverHerId) + otherReceivers).distinct(),
            messageTypeIdentificator = type
        )
    }

    private fun herId(
        organisation: XMLOrganisation?,
        field: String,
        practitioner: XMLHealthcareProfessional? = null
    ): Either<ConversionError, String> = either {
        val herIds = organisation.identifierCandidates(practitioner)
            .map { it.herIds() }
            .firstOrNull { it.isNotEmpty() }
            ?: raise(missing("$field.herId"))
        ensure(herIds.size == 1) { MappingError("Ambiguous HER identifiers in MsgHead field: $field", "$field.herId") }
        herIds.single()
    }

    private fun XMLOrganisation?.identifierCandidates(
        practitioner: XMLHealthcareProfessional?
    ): Sequence<List<XMLIdent>> =
        generateSequence(this) { it.organisation }
            .toList()
            .asReversed()
            .let { organisations ->
                // Search all practitioner levels before falling back to organizations.
                sequenceOf(practitioner?.ident).filterNotNull() +
                    organisations.asSequence().mapNotNull { it.healthcareProfessional?.ident } +
                    organisations.asSequence().map { it.ident }
            }

    private fun List<XMLIdent>.herIds(): List<String> =
        filter { it.typeId?.v == "HER" }
            .mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }
            .distinct()

    private fun missing(field: String) = MappingError("Missing required MsgHead field: $field", field)
}

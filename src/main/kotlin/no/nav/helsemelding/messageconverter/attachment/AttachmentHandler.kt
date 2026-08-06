package no.nav.helsemelding.messageconverter.attachment

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.model.Attachment
import no.nav.helsemelding.messageconverter.model.SplitMessage

interface AttachmentHandler {
    fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage>
    fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>>
    fun removeAttachments(msgHeadXml: String): Either<ConversionError, String>
}

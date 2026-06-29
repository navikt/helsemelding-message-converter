package no.nav.helsemelding.message.attachment

import arrow.core.Either
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.model.Attachment
import no.nav.helsemelding.message.model.SplitMessage

interface AttachmentHandler {
    fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage>
    fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>>
    fun removeAttachments(msgHeadXml: String): Either<ConversionError, String>
}

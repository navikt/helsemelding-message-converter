package no.nav.helsemelding.message.converter

import arrow.core.Either
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.message.error.AdditionalMessageInfoError
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.msghead.model.AdditionalMessageInfo
import kotlin.uuid.Uuid

/**
 * Provides additional metadata needed to construct the MsgHead envelope for outgoing dialog messages.
 *
 * Implement this interface to supply message-specific information that is not available
 * in the dialog message JSON itself.
 */
interface AdditionalMessageInfoProvider {
    /**
     * Returns additional metadata for the given outgoing dialog message.
     *
     * @param dialogMessage the outgoing dialog message to look up metadata for
     * @return [AdditionalMessageInfo] required for MsgHead construction, or a [ConversionError] on failure
     */
    fun getAdditionalMessageInfo(dialogMessage: OutgoingDialogMessage): Either<ConversionError, AdditionalMessageInfo>
}

/**
 * Default [AdditionalMessageInfoProvider] used when no provider has been configured.
 *
 * Always returns an [AdditionalMessageInfoError], ensuring that outgoing conversion
 * fails with a clear error rather than silently producing incorrect output.
 */
class MissingAdditionalMessageInfoProvider : AdditionalMessageInfoProvider {
    override fun getAdditionalMessageInfo(dialogMessage: OutgoingDialogMessage): Either<ConversionError, AdditionalMessageInfo> {
        return Either.Left(
            AdditionalMessageInfoError("AdditionalMessageInfoProvider is required for outgoing conversion")
        )
    }
}

class FakeAdditionalMessageInfoProvider : AdditionalMessageInfoProvider {
    private val additionalMessageInfoByMsgId = mutableMapOf<Uuid, Either<ConversionError, AdditionalMessageInfo>>()

    fun givenAdditionalMessageInfo(msgId: Uuid, either: Either<ConversionError, AdditionalMessageInfo>) {
        additionalMessageInfoByMsgId[msgId] = either
    }

    override fun getAdditionalMessageInfo(dialogMessage: OutgoingDialogMessage): Either<ConversionError, AdditionalMessageInfo> {
        return additionalMessageInfoByMsgId[Uuid.parse(dialogMessage.id)]
            ?: Either.Left(AdditionalMessageInfoError("Error when fetching additional message info"))
    }
}

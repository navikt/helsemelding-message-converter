package no.nav.helsemelding.messageconverter

import arrow.core.Either
import no.nav.helsemelding.jsonschema.core.model.OutgoingDialogMessage
import no.nav.helsemelding.messageconverter.error.AdditionalMessageInfoError
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.msghead.model.AdditionalMessageInfo
import kotlin.uuid.Uuid

interface AdditionalMessageInfoProvider {
    fun getAdditionalMessageInfo(dialogMessage: OutgoingDialogMessage): Either<ConversionError, AdditionalMessageInfo>
}

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

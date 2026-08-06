package no.nav.helsemelding.messageconverter.msghead.model

import no.nav.helsemelding.messageconverter.msghead.model.provider.Provider
import java.time.Instant
import kotlin.uuid.Uuid

data class AdditionalMessageInfo(
    val provider: Provider,
    val employee: Employee,
    val createdAt: Instant = Instant.now(),
    val docId: Uuid = Uuid.random()
)

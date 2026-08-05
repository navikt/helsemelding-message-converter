package no.nav.helsemelding.message.msghead.model

import no.nav.helsemelding.message.msghead.model.provider.Provider
import java.time.Instant
import kotlin.uuid.Uuid

data class AdditionalMessageInfo(
    val provider: Provider,
    val employee: Employee,
    val createdAt: Instant = Instant.now(),
    val docId: Uuid = Uuid.random()
)

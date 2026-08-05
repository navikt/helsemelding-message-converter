package no.nav.helsemelding.message.msghead.model

import no.nav.helsemelding.message.msghead.model.provider.Provider
import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class AdditionalMessageInfo(
    val provider: Provider,
    val employee: Employee,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val docId: Uuid = Uuid.random()
)

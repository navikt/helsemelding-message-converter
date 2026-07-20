package no.nav.helsemelding.message.msghead.model

import no.nav.helsemelding.message.client.providerregistry.model.Provider
import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class AdditionalMessageInfo(
    val provider: Provider,
    val employee: Employee,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val dokId: Uuid = Uuid.random()
)

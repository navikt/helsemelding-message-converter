package no.nav.helsemelding.message.msghead.adapter

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

class DateTimeAdapter : LocalDateTimeXmlAdapter() {
    override fun unmarshal(value: String?): LocalDateTime? =
        value?.let(::parseDateTime)

    private fun parseDateTime(value: String): LocalDateTime =
        runCatching {
            ZonedDateTime.parse(value)
                .withZoneSameInstant(OSLO_ZONE)
                .toLocalDateTime()
        }
            .getOrElse { LocalDateTime.parse(value) }
}

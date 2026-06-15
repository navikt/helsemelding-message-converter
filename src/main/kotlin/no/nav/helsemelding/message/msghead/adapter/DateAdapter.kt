package no.nav.helsemelding.message.msghead.adapter

import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import java.time.LocalDate
import java.time.ZoneOffset
import javax.xml.bind.DatatypeConverter

class DateAdapter : LocalDateXmlAdapter() {
    override fun unmarshal(value: String?): LocalDate? =
        value?.let {
            DatatypeConverter.parseDate(it)
                .toInstant()
                .atZone(ZoneOffset.MAX)
                .toLocalDate()
        }
}

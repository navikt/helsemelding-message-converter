package no.nav.helsemelding.messageconverter.msghead.model.provider

import kotlinx.serialization.SerialName

enum class ProviderCategory(
    val categoryCode: String
) {
    @SerialName("FYSIOTERAPEUT")
    PHYSIOTHERAPIST("FT"),

    @SerialName("KIROPRAKTOR")
    CHIROPRACTOR("KI"),

    @SerialName("LEGE")
    DOCTOR("LE"),

    @SerialName("MANUELLTERAPEUT")
    MANUAL_THERAPIST("MT"),

    @SerialName("TANNLEGE")
    DENTIST("TL"),

    @SerialName("PSYKOLOG")
    PSYCHOLOGIST("PS");
}

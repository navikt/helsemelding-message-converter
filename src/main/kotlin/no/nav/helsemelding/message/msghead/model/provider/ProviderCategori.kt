package no.nav.helsemelding.message.msghead.model.provider

enum class ProviderCategori(
    val kategoriKode: String
) {
    FYSIOTERAPEUT("FT"),
    KIROPRAKTOR("KI"),
    LEGE("LE"),
    MANUELLTERAPEUT("MT"),
    TANNLEGE("TL"),
    PSYKOLOG("PS");
}

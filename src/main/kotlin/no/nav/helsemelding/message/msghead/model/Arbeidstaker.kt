package no.nav.helsemelding.message.msghead.model

data class Arbeidstaker(
    val personident: Personident,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
)

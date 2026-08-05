package no.nav.helsemelding.message.msghead.model

data class Employee(
    val personident: Personident,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String
)

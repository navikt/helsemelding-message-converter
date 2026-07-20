package no.nav.helsemelding.message.client.providerregistry.model

import kotlinx.serialization.Serializable

@Serializable
data class OrganisationNumber(val value: String) {
    init {
        if (!nineDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid organisation number")
        }
    }

    companion object {
        val nineDigits = Regex("^\\d{9}\$")
    }
}

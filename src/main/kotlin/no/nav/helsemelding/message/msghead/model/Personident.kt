package no.nav.helsemelding.message.msghead.model

import kotlinx.serialization.Serializable

@Serializable
data class Personident(val value: String) {
    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("Value($value) is not a valid Personident")
        }
    }

    companion object {
        val elevenDigits = Regex("^\\d{11}\$")
    }
}

fun Personident.isDNR() = this.value[0].digitToInt() > 3

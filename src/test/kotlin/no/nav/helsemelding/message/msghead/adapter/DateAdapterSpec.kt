package no.nav.helsemelding.message.msghead.adapter

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DateAdapterSpec : StringSpec(
    {
        val adapter = DateAdapter()

        "should return null when input is null" {
            adapter.unmarshal(null) shouldBe null
        }

        "should parse date without timezone" {
            val result = adapter.unmarshal("2026-06-03")

            result shouldBe LocalDate.of(2026, 6, 3)
        }

        "should parse date with timezone" {
            val result = adapter.unmarshal("2026-06-03+02:00")

            result shouldBe LocalDate.of(2026, 6, 3)
        }
    }
)

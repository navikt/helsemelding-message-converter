package no.nav.helsemelding.messageconverter

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLOtherReceiver
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLSender
import no.nav.helsemelding.messageconverter.error.MappingError

class MsgHeadMetadataExtractorSpec : StringSpec(
    {
        val extractor = MsgHeadMetadataExtractor()

        "should prefer deepest practitioner even when a deeper organisation has a her id" {
            val sender = organisation(
                "office",
                practitioner("parent"),
                organisation("unit", practitioner("child"), organisation("leaf"))
            )
            val msgHead = message(sender = sender)

            val metadata = extractor.extract(msgHead).shouldBeRight()

            metadata.senderHerId shouldBe "child"
        }

        "should fall back to most specific organisation when practitioners have no her id" {
            val sender = organisation(
                "parent",
                child = organisation("child", practitioner("", "HPR"))
            )
            val msgHead = message(sender = sender)

            val metadata = extractor.extract(msgHead).shouldBeRight()

            metadata.senderHerId shouldBe "child"
        }

        "should include other receivers and remove duplicates in encounter order" {
            val others = listOf(
                XMLOtherReceiver().apply { organisation = organisation("office", practitioner("other")) },
                XMLOtherReceiver().apply { healthcareProfessional = practitioner("direct") },
                XMLOtherReceiver().apply { organisation = organisation("receiver") }
            )
            val msgHead = message(others = others)

            val metadata = extractor.extract(msgHead).shouldBeRight()

            metadata.receiverHerIds shouldBe listOf("receiver", "other", "direct")
        }

        "should select her id by type regardless of identifier order and preserve leading zeros" {
            val sender = XMLOrganisation().apply {
                ident.addAll(listOf(identifier("wrong", "ENH"), identifier("00123"), identifier("00123")))
            }
            val msgHead = message(sender = sender)

            val metadata = extractor.extract(msgHead).shouldBeRight()

            metadata.senderHerId shouldBe "00123"
        }

        "should accept unknown message types without dialog content" {
            val msgHead = message(type = "FUTURE_TYPE")

            val metadata = extractor.extract(msgHead).shouldBeRight()

            metadata.messageTypeIdentificator shouldBe "FUTURE_TYPE"
        }

        withData(
            nameFn = { "should return MappingError when ${it.first}" },
            listOf(
                Triple("MsgInfo is missing", XMLMsgHead(), "msgInfo"),
                Triple("message type is blank", message(type = " "), "msgInfo.type.v"),
                Triple("message type is missing", message().apply { msgInfo.type = null }, "msgInfo.type.v"),
                Triple("sender her id is blank", message(sender = organisation(" ")), "msgInfo.sender.herId"),
                Triple(
                    "receiver has no her id",
                    message(receiver = organisation("", practitioner("123", "HPR"))),
                    "msgInfo.receiver.herId"
                ),
                Triple("receiver is missing", message().apply { msgInfo.receiver = null }, "msgInfo.receiver.herId"),
                Triple(
                    "additional receiver has no her id",
                    message(others = listOf(XMLOtherReceiver())),
                    "msgInfo.otherReceiver[0].herId"
                )
            )
        ) { (_, msgHead, field) ->
            val error = extractor.extract(msgHead).shouldBeLeft()

            error.shouldBeInstanceOf<MappingError>()
            error.field shouldBe field
        }

        "should return MappingError for ambiguous practitioner identifiers without falling back" {
            val practitioner = practitioner("one").apply { ident.add(identifier("two")) }
            val sender = organisation("office", practitioner)
            val msgHead = message(sender = sender)

            val error = extractor.extract(msgHead).shouldBeLeft()

            error.shouldBeInstanceOf<MappingError>()
            error.field shouldBe "msgInfo.sender.herId"
        }
    }
)

private fun identifier(value: String, type: String = "HER") = XMLIdent().apply {
    id = value
    typeId = XMLCV().apply { v = type }
}

private fun practitioner(id: String, type: String = "HER") = XMLHealthcareProfessional().apply {
    ident.add(identifier(id, type))
}

private fun organisation(
    id: String,
    practitioner: XMLHealthcareProfessional? = null,
    child: XMLOrganisation? = null
) = XMLOrganisation().apply {
    ident.add(identifier(id))
    healthcareProfessional = practitioner
    organisation = child
}

private fun message(
    sender: XMLOrganisation = organisation("sender"),
    receiver: XMLOrganisation = organisation("receiver"),
    others: List<XMLOtherReceiver> = emptyList(),
    type: String = "DIALOG_NOTAT"
) = XMLMsgHead().apply {
    msgInfo = XMLMsgInfo().apply {
        this.type = XMLCS().apply { v = type }
        this.sender = XMLSender().apply { organisation = sender }
        this.receiver = XMLReceiver().apply { organisation = receiver }
        otherReceiver.addAll(others)
    }
}

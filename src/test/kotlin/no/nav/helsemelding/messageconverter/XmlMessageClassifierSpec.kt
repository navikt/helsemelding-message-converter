package no.nav.helsemelding.messageconverter

import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helsemelding.messageconverter.error.InvalidXml

class XmlMessageClassifierSpec : StringSpec(
    {
        "identifies AppRec without a namespace" {
            XmlMessageClassifier.isAppRec("<AppRec/>") shouldBe Right(true)
        }

        "identifies AppRec with a default namespace" {
            XmlMessageClassifier.isAppRec(
                """<AppRec xmlns="http://www.kith.no/xmlstds/apprec/2004-11-21"/>"""
            ) shouldBe Right(true)
        }

        "identifies AppRec with a namespace prefix" {
            XmlMessageClassifier.isAppRec(
                """<app:AppRec xmlns:app="http://www.kith.no/xmlstds/apprec/2004-11-21"/>"""
            ) shouldBe Right(true)
        }

        "identifies AppRec with an XML declaration, comment and message content" {
            val xml = """
                <?xml version="1.0"?>
                <!-- AppRec -->
                <a:AppRec xmlns:a="http://www.kith.no/xmlstds/apprec/2004-11-21">
                    <a:MsgType V="APPREC" DN="Applikasjonskvittering"/>
                </a:AppRec>
            """.trimIndent()

            XmlMessageClassifier.isAppRec(xml) shouldBe Right(true)
        }

        "returns false for another root element" {
            XmlMessageClassifier.isAppRec("<MsgHead/>") shouldBe Right(false)
        }

        "returns false when AppRec is not the root element" {
            XmlMessageClassifier.isAppRec("<MsgHead><AppRec/></MsgHead>") shouldBe Right(false)
        }

        "returns InvalidXml for malformed XML" {
            val result = XmlMessageClassifier.isAppRec("<AppRec>")

            result.shouldBeInstanceOf<Left<InvalidXml>>()
        }

        "returns InvalidXml for empty input" {
            val result = XmlMessageClassifier.isAppRec("")

            result.shouldBeInstanceOf<Left<InvalidXml>>()
        }

        "rejects XML containing a DOCTYPE declaration" {
            val result = XmlMessageClassifier.isAppRec("<!DOCTYPE AppRec><AppRec/>")

            result.shouldBeInstanceOf<Left<InvalidXml>>()
        }
    }
)

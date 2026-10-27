package no.nav.helsemelding.messageconverter

import arrow.core.Either
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.InvalidXml
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object XmlMessageClassifier {
    /**
     * Checks whether the XML root element has the local name AppRec.
     *
     * This does not validate the AppRec schema or namespace.
     * A false result does not imply that the XML is a valid MsgHead message.
     *
     * @param xml the raw XML string
     * @return whether the root element is AppRec, or [InvalidXml] if parsing fails
     */
    fun isAppRec(xml: String): Either<ConversionError, Boolean> = Either.catch {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = factory.newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))

        document.documentElement.localName == "AppRec"
    }
        .mapLeft { InvalidXml("Could not parse XML when checking for AppRec", it) }
}

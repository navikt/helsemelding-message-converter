package no.nav.helsemelding.message.msghead

import arrow.core.Either
import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.base64container.Base64Container
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helsemelding.message.error.ConversionError
import no.nav.helsemelding.message.error.InvalidXml
import no.nav.helsemelding.message.error.SerializationError
import no.nav.helsemelding.message.msghead.adapter.DateAdapter
import no.nav.helsemelding.message.msghead.adapter.DateTimeAdapter
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

const val DISALLOW_DOCTYPE_DECLARATION = "http://apache.org/xml/features/disallow-doctype-decl"

class XmlSerializer {
    private val msgHeadJaxBContext: JAXBContext = JAXBContext.newInstance(
        XMLMsgHead::class.java,
        XMLDialogmelding::class.java,
        Base64Container::class.java,
        XMLAppRec::class.java
    )

    fun deserialize(value: String): Either<ConversionError, XMLMsgHead> =
        Either.catch {
            val parserFactory = configureParserFactory()
            val unmarshaller = configureUnmarshaller()

            val xmlSource: Source = SAXSource(
                parserFactory.newSAXParser().xmlReader,
                InputSource(StringReader(value))
            )

            unmarshaller.unmarshal(xmlSource) as XMLMsgHead
        }
            .mapLeft { InvalidXml("Could not deserialize MsgHead XML", it) }

    fun serialize(msgHead: XMLMsgHead): Either<ConversionError, String> =
        Either.catch {
            val stringWriter = StringWriter()
            val marshaller = configureMarshaller()

            marshaller.marshal(msgHead, stringWriter)

            stringWriter.toString()
        }
            .mapLeft { SerializationError("Could not serialize MsgHead XML", it) }

    private fun configureUnmarshaller(): Unmarshaller =
        msgHeadJaxBContext.createUnmarshaller().apply {
            setAdapter(LocalDateTimeXmlAdapter::class.java, DateTimeAdapter())
            setAdapter(LocalDateXmlAdapter::class.java, DateAdapter())
        }

    private fun configureMarshaller(): Marshaller =
        msgHeadJaxBContext.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        }

    private fun configureParserFactory(): SAXParserFactory {
        val parserFactory = SAXParserFactory.newInstance()
        parserFactory.setFeature(DISALLOW_DOCTYPE_DECLARATION, true)
        parserFactory.isNamespaceAware = true

        return parserFactory
    }
}

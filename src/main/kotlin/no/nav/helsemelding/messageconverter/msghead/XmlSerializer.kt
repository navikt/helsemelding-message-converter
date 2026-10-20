package no.nav.helsemelding.messageconverter.msghead

import arrow.core.Either
import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.base64container.Base64Container
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLPatient
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.InvalidXml
import no.nav.helsemelding.messageconverter.error.SerializationError
import no.nav.helsemelding.messageconverter.msghead.adapter.DateAdapter
import no.nav.helsemelding.messageconverter.msghead.adapter.DateTimeAdapter
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.namespace.QName
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

const val DISALLOW_DOCTYPE_DECLARATION = "http://apache.org/xml/features/disallow-doctype-decl"
private const val MSG_HEAD_NAMESPACE = "http://www.kith.no/xmlstds/msghead/2006-05-24"

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

    internal fun serializeForContentHash(dialog: XMLDialogmelding): Either<ConversionError, String> =
        serializePart(dialog, XMLDialogmelding::class.java, "Dialogmelding")

    internal fun serializeForContentHash(patient: XMLPatient): Either<ConversionError, String> =
        serializePart(patient, XMLPatient::class.java, "Patient")

    internal fun serializeForContentHash(document: XMLDocument): Either<ConversionError, String> =
        serializePart(document, XMLDocument::class.java, "Document")

    private fun <T> serializePart(
        value: T,
        type: Class<T>,
        localName: String
    ): Either<ConversionError, String> =
        Either.catch {
            val stringWriter = StringWriter()
            val marshaller = configureMarshaller()
            val element = JAXBElement(QName(MSG_HEAD_NAMESPACE, localName), type, value)

            marshaller.marshal(element, stringWriter)

            stringWriter.toString()
        }
            .mapLeft { SerializationError("Could not serialize MsgHead content for hashing", it) }

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

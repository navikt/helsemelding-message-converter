package no.nav.helsemelding.messageconverter.error

/**
 * Sealed interface representing all errors that can occur during message conversion.
 *
 * @property message a description of what went wrong
 * @property cause the underlying exception, if any
 */
sealed interface ConversionError {
    val message: String
    val cause: Throwable?
}

/**
 * The input XML is not valid or cannot be parsed.
 *
 * @property message description of the XML parsing error (required)
 * @property cause the underlying exception
 */
data class InvalidXml(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

/**
 * The input JSON is not valid or cannot be parsed.
 *
 * @property message description of the JSON parsing error (required)
 * @property cause the underlying exception
 */
data class InvalidJson(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

/**
 * A field could not be mapped between formats.
 *
 * @property message description of the mapping error (required)
 * @property field the name of the field that caused the error
 * @property cause the underlying exception
 */
data class MappingError(
    override val message: String,
    val field: String? = null,
    override val cause: Throwable? = null
) : ConversionError

/**
 * Serialization to the target format failed.
 *
 * @property message description of the serialization error (required)
 * @property cause the underlying exception
 */
data class SerializationError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

/**
 * An attachment could not be extracted or processed.
 *
 * @property message description of the attachment error (required)
 * @property cause the underlying exception
 */
data class AttachmentError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

/**
 * An expected attachment was not found in the message.
 *
 * @property message description of what attachment was missing (required)
 * @property cause the underlying exception
 */
data class AttachmentMissingError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

/**
 * Additional message info required for outgoing conversion could not be retrieved.
 *
 * @property message description of the error (required)
 * @property cause the underlying exception
 */
data class AdditionalMessageInfoError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

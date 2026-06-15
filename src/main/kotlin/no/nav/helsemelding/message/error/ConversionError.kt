package no.nav.helsemelding.message.error

sealed interface ConversionError {
    val message: String
    val cause: Throwable?
}

data class InvalidXml(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

data class InvalidJson(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

data class MappingError(
    override val message: String,
    val field: String? = null,
    override val cause: Throwable? = null
) : ConversionError

data class SerializationError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

data class AttachmentError(
    override val message: String,
    override val cause: Throwable? = null
) : ConversionError

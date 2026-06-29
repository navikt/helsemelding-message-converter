# helsemelding-message-converter

Library for converting helsemelding dialog messages between MsgHead XML and the JSON message models from `json-schema-core`.

The library currently supports two conversion directions:

- Incoming dialog message: MsgHead XML to `IncomingDialogMessage` JSON
- Outgoing dialog message: `OutgoingDialogMessage` JSON to MsgHead XML

It also includes helpers for extracting and removing attachments from MsgHead XML.

## Public API

Use `MsgHeadMessageConverter` for MsgHead-based conversions:

```kotlin
import no.nav.helsemelding.message.converter.MsgHeadMessageConverter

val converter = MsgHeadMessageConverter()
```

The converter implements:

```kotlin
interface MessageConverter {
    fun incomingDialogMessageXmlToJson(xml: String): Either<ConversionError, String>
    fun outgoingDialogMessageJsonToXml(json: String): Either<ConversionError, String>
}
```

It also implements `AttachmentHandler`:

```kotlin
interface AttachmentHandler {
    fun extractAttachments(msgHeadXml: String): Either<ConversionError, List<Attachment>>
    fun removeAttachments(msgHeadXml: String): Either<ConversionError, String>
}
```

## Convert Incoming MsgHead XML To JSON

```kotlin
import arrow.core.getOrElse
import no.nav.helsemelding.message.converter.MsgHeadMessageConverter

val converter = MsgHeadMessageConverter()

val json = converter
    .incomingDialogMessageXmlToJson(msgHeadXml)
    .getOrElse { error ->
        error("Could not convert incoming dialog message: ${error.message}")
    }
```

The resulting JSON follows the `IncomingDialogMessage` schema from `json-schema-core`.

## Convert Outgoing JSON To MsgHead XML

```kotlin
import arrow.core.getOrElse
import no.nav.helsemelding.message.converter.MsgHeadMessageConverter

val converter = MsgHeadMessageConverter()

val xml = converter
    .outgoingDialogMessageJsonToXml(outgoingDialogMessageJson)
    .getOrElse { error ->
        error("Could not convert outgoing dialog message: ${error.message}")
    }
```

The input JSON must follow the `OutgoingDialogMessage` schema from `json-schema-core`.

## Attachments

Attachments can be handled separately from conversion:

```kotlin
val attachments = converter.extractAttachments(msgHeadXml)
val xmlWithoutAttachments = converter.removeAttachments(msgHeadXml)
```

## Errors

All public operations return `Either<ConversionError, T>`.

Possible error types:

- `InvalidXml`
- `InvalidJson`
- `MappingError`
- `SerializationError`
- `AttachmentError`

Example:

```kotlin
converter.incomingDialogMessageXmlToJson(msgHeadXml).fold(
    { error -> println("Conversion failed: ${error.message}") },
    { json -> println(json) }
)
```
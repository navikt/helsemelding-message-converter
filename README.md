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
    fun splitAttachments(msgHeadXml: String): Either<ConversionError, SplitMessage>
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

### Resources used
- Link to original spec at nhn? - add PDF to repo?
This makes use of code generated based on xsd definitions from the following [repository](https://github.com/navikt/syfo-xml-codegen)

Supported outgoing and incoming dialog messages are listed in the table below:
![Overview](dialogMessageOverview.png)

### Outgoing dialog message
- Link to test folder with examples
- Table over supported types? Highlight minor differences between some types when it comes to input JSON.
Complete examples of every possible outgoing message can be found in [test folder](./src/test/resources/msghead)

All outgoing messages are based on `OutgoingDialogMessage`, however some information is not used depending on `OutgoingDialogMessageType`

#### FollowUpPlanMessage

- Provided ConversationRef is ignored and not included in final message
- Requires an attachment 
- Provided message text is ignored and replaced with hardcoded value in final message

#### InquiryMessage and MemoMessage

- ConversationReference is used if provided. 
- If no conversationReference is provided that means this is initial message in a conversation. This results in the 
following behavior:
  - Provided message id is used as parentMessageId and conversationId

## Attachments

Attachments can be handled separately from conversion:

```kotlin
val splitMessage = converter.splitAttachments(msgHeadXml)
val attachments = converter.extractAttachments(msgHeadXml)
val xmlWithoutAttachments = converter.removeAttachments(msgHeadXml)
```

`splitAttachments` returns the XML with attachments removed and the extracted attachments in one operation:

```kotlin
data class SplitMessage(
    val messageWithoutAttachmentsXml: String,
    val attachments: List<Attachment>
)
```

`Attachment` contains:

```kotlin
data class Attachment(
    val description: String,
    val contentType: String,
    val contentBase64: String
)
```

Supported attachment MIME types are PDF, TIFF, PNG, JPEG, PJPEG, JPG, and PJPG.

## Errors

All public operations return `Either<ConversionError, T>`.

Possible error types:

- `InvalidXml`
- `InvalidJson`
- `MappingError`
- `SerializationError`
- `AttachmentError`
- `AttachmentMissingError`
- `AdditionalMessageInfoError`

Example:

```kotlin
converter.incomingDialogMessageXmlToJson(msgHeadXml).fold(
    { error -> println("Conversion failed: ${error.message}") },
    { json -> println(json) }
)
```

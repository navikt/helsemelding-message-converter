package no.nav.helsemelding.messageconverter.msghead

import arrow.core.Either
import arrow.core.raise.either
import no.nav.helse.dialogmelding.XMLDialogmelding
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLPatient
import no.nav.helsemelding.messageconverter.error.AttachmentError
import no.nav.helsemelding.messageconverter.error.ConversionError
import no.nav.helsemelding.messageconverter.error.MappingError
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

private const val MISSING_DIALOG_FIELD = "document[0].refDoc.content.Dialogmelding"
private const val MISSING_PATIENT_FIELD = "msgInfo.patient"

internal class MsgHeadContentHasher(private val xmlSerializer: XmlSerializer) {
    fun calculate(xml: String): Either<ConversionError, String> =
        either {
            val msgHead = xmlSerializer.deserialize(xml).bind()

            val dialogMessageHash = calculateDialogMessageHash(msgHead).bind()
            val patientHash = calculatePatientHash(msgHead).bind()
            val attachmentHash = calculateAttachmentHash(msgHead).bind()

            val componentHashes = buildList {
                add(dialogMessageHash)
                add(patientHash)
                add(attachmentHash)
            }

            componentHashes.joinToString(separator = "").sha256Hex()
        }

    private fun calculateDialogMessageHash(msgHead: XMLMsgHead): Either<ConversionError, String> =
        either {
            val dialogMessages = msgHead.dialogMessages()
            if (dialogMessages.isEmpty()) {
                raise(
                    MappingError(
                        message = "Missing required MsgHead field: $MISSING_DIALOG_FIELD",
                        field = MISSING_DIALOG_FIELD
                    )
                )
            }

            dialogMessages.joinToString(separator = "") {
                xmlSerializer.serializeForContentHash(it).bind()
            }.sha256Hex()
        }

    private fun calculatePatientHash(msgHead: XMLMsgHead): Either<ConversionError, String> =
        either {
            val patient = msgHead.patientOrNested()
                ?: raise(
                    MappingError(
                        message = "Missing required MsgHead field: $MISSING_PATIENT_FIELD",
                        field = MISSING_PATIENT_FIELD
                    )
                )

            xmlSerializer.serializeForContentHash(patient).bind().sha256Hex()
        }

    private fun calculateAttachmentHash(msgHead: XMLMsgHead): Either<ConversionError, String> =
        either {
            Either.catch { msgHead.extractAttachmentDocuments() }
                .mapLeft {
                    AttachmentError("Could not extract attachment documents from MsgHead", it)
                }
                .bind()
                .joinToString(separator = "") {
                    xmlSerializer.serializeForContentHash(it).bind()
                }.sha256Hex()
        }
}

private fun XMLMsgHead.dialogMessages(): List<XMLDialogmelding> =
    buildList {
        document.forEach { document ->
            document.refDoc?.content?.any?.forEach { content ->
                when (content) {
                    is XMLDialogmelding -> add(content)
                    is XMLMsgHead -> addAll(content.dialogMessages())
                }
            }
        }
    }

private fun XMLMsgHead.patientOrNested(): XMLPatient? =
    msgInfo?.patient ?: document
        .asSequence()
        .flatMap { document -> document.refDoc?.content?.any?.asSequence() ?: emptySequence() }
        .filterIsInstance<XMLMsgHead>()
        .mapNotNull(XMLMsgHead::patientOrNested)
        .firstOrNull()

private fun String.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(UTF_8))
        .toLowercaseHex()

private fun ByteArray.toLowercaseHex(): String {
    val hexDigits = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toLowercaseHex) {
            val value = byte.toInt() and 0xff
            append(hexDigits[value shr 4])
            append(hexDigits[value and 0x0f])
        }
    }
}

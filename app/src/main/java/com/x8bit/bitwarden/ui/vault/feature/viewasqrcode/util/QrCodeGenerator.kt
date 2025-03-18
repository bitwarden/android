package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeConfig
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import java.net.URLEncoder

/**
 * Utility class for generating QR codes.
 */
@OmitFromCoverage
object QrCodeGenerator {

    private const val QR_CODE_SIZE = 512
    private const val UTF_8 = "UTF-8"

    /**
     * Generate a QR code bitmap from the given configuration.
     *
     * @param config The QR code configuration.
     * @return A bitmap containing the generated QR code.
     */
    fun generateQrCode(config: QrCodeConfig): Bitmap {
        val content = formatQrCodeContent(config)
        return generateQrCodeBitmap(content)
    }

    /**
     * Format the content for the QR code based on the configuration.
     *
     * @param config The QR code configuration.
     * @return The formatted content string for the QR code.
     */
    private fun formatQrCodeContent(config: QrCodeConfig): String {
        return when (config.type) {
            QrCodeType.PlainText -> config.fields["text"] ?: ""
            QrCodeType.Url -> config.fields["url"] ?: ""
            QrCodeType.Email -> {
                val email = config.fields["email"] ?: ""
                val subject = config.fields["subject"] ?: ""
                val body = config.fields["body"] ?: ""

                if (subject.isNotEmpty() || body.isNotEmpty()) {
                    val encodedSubject = URLEncoder.encode(subject, UTF_8)
                    val encodedBody = URLEncoder.encode(body, UTF_8)
                    "mailto:$email?subject=$encodedSubject&body=$encodedBody"
                } else {
                    "mailto:$email"
                }
            }
            QrCodeType.Phone -> "tel:${config.fields["phone"] ?: ""}"
            QrCodeType.SMS -> {
                val phone = config.fields["phone"] ?: ""
                val message = config.fields["message"] ?: ""

                if (message.isNotEmpty()) {
                    val encodedMessage = URLEncoder.encode(message, UTF_8)
                    "smsto:$phone:$encodedMessage"
                } else {
                    "smsto:$phone"
                }
            }
            QrCodeType.WiFi -> {
                val ssid = config.fields["ssid"] ?: ""
                val password = config.fields["password"] ?: ""
                val type = config.fields["type"] ?: "WPA"
                val hidden = config.fields["hidden"] == "true"

                "WIFI:S:$ssid;T:$type;P:$password;H:$hidden;;"
            }
            QrCodeType.Contact -> {
                val name = config.fields["name"] ?: ""
                val phone = config.fields["phone"] ?: ""
                val email = config.fields["email"] ?: ""
                val organization = config.fields["organization"] ?: ""
                val address = config.fields["address"] ?: ""

                buildString {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    if (name.isNotEmpty()) append("N:$name\n")
                    if (name.isNotEmpty()) append("FN:$name\n")
                    if (organization.isNotEmpty()) append("ORG:$organization\n")
                    if (phone.isNotEmpty()) append("TEL:$phone\n")
                    if (email.isNotEmpty()) append("EMAIL:$email\n")
                    if (address.isNotEmpty()) append("ADR:;;$address\n")
                    append("END:VCARD")
                }
            }
        }
    }

    /**
     * Generate a QR code bitmap from the given content string.
     *
     * @param content The content to encode in the QR code.
     * @return A bitmap containing the generated QR code.
     */
    private fun generateQrCodeBitmap(content: String): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to UTF_8,
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE,
            hints
        )

        return createBitmapFromBitMatrix(bitMatrix)
    }

    /**
     * Create a bitmap from a ZXing BitMatrix.
     *
     * @param bitMatrix The BitMatrix to convert.
     * @return A bitmap representation of the BitMatrix.
     */
    private fun createBitmapFromBitMatrix(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
}

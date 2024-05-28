package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.util

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * A class setup to handle image analysis so that we can use the Zxing library
 * to scan QR codes and convert them to a string.
 */
class QrCodeAnalyzerImpl : QrCodeAnalyzer {

    /**
     * This will ensure the result is only sent once as multiple images with a valid
     * QR code can be sent for analysis.
     */
    private var qrCodeRead = false

    override lateinit var onQrCodeScanned: (String) -> Unit

    override fun analyze(image: ImageProxy) {
        if (qrCodeRead) {
            return
        }

        val source = PlanarYUVLuminanceSource(
            image.planes[0].buffer.toByteArray(),
            image.planes[0].rowStride,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false,
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = MultiFormatReader().decode(
                /* image = */ binaryBitmap,
                /* hints = */
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.ALSO_INVERTED to true
                ),
            )

            qrCodeRead = true
            onQrCodeScanned(result.text)
        } catch (ignored: NotFoundException) {

        } finally {
            image.close()
        }
    }
}

/**
 * This function helps us prepare the byte buffer to be read.
 */
private fun ByteBuffer.toByteArray(): ByteArray =
    ByteArray(rewind().remaining()).also { get(it) }

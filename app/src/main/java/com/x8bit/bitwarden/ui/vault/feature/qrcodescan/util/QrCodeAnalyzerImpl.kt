package com.x8bit.bitwarden.ui.vault.feature.qrcodescan.util

import androidx.camera.core.ImageProxy
import com.bitwarden.core.annotation.OmitFromCoverage
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
@OmitFromCoverage
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
            image.width,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false,
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = MultiFormatReader()
                .apply {
                    setHints(
                        mapOf(
                            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(
                                BarcodeFormat.QR_CODE,
                            ),
                        ),
                    )
                }
                .decode(binaryBitmap)

            qrCodeRead = true
            onQrCodeScanned(result.text)
        } catch (e: NotFoundException) {
            return
        } finally {
            image.close()
        }
    }
}

/**
 * This function helps us prepare the byte buffer to be read.
 */
@OmitFromCoverage
private fun ByteBuffer.toByteArray(): ByteArray =
    ByteArray(rewind().remaining()).also { get(it) }

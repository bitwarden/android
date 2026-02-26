package com.bitwarden.ui.platform.feature.qrcodescan.util

import androidx.camera.core.ImageProxy
import com.bitwarden.annotation.OmitFromCoverage
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
     * This will ensure that only 1 QR code is analyzed at a time.
     */
    private var isQrCodeInAnalysis: Boolean = false

    override lateinit var onQrCodeScanned: (String) -> Unit

    override fun analyze(image: ImageProxy) {
        if (isQrCodeInAnalysis) return
        isQrCodeInAnalysis = true

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
            val result = MultiFormatReader().decode(
                binaryBitmap,
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.ALSO_INVERTED to true,
                ),
            )

            onQrCodeScanned(result.text)
        } catch (_: NotFoundException) {
            return
        } finally {
            image.close()
            isQrCodeInAnalysis = false
        }
    }
}

/**
 * This function helps us prepare the byte buffer to be read.
 */
@OmitFromCoverage
private fun ByteBuffer.toByteArray(): ByteArray =
    ByteArray(rewind().remaining()).also { get(it) }

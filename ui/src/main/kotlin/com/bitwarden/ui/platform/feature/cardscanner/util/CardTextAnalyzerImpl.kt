package com.bitwarden.ui.platform.feature.cardscanner.util

import androidx.camera.core.ImageProxy
import com.bitwarden.annotation.OmitFromCoverage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [CardTextAnalyzer] implementation that uses ML Kit Text Recognition
 * to detect credit card details from camera frames.
 *
 * @property cardDataParser The parser used to extract card data from
 * recognized text.
 */
@OmitFromCoverage
class CardTextAnalyzerImpl(
    private val cardDataParser: CardDataParser = CardDataParserImpl(),
) : CardTextAnalyzer {

    private val isInAnalysis = AtomicBoolean(false)

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS,
    )

    override lateinit var onCardScanned: (CardScanData) -> Unit

    @Suppress("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (!isInAnalysis.compareAndSet(false, true)) {
            image.close()
            return
        }

        val inputImage = image.image
            ?.let {
                InputImage.fromMediaImage(
                    it,
                    image.imageInfo.rotationDegrees,
                )
            }
            ?: run {
                image.close()
                isInAnalysis.set(false)
                return
            }

        recognizer.process(inputImage)
            .addOnSuccessListener { result ->
                result.text
                    .let(cardDataParser::parseCardData)
                    .takeIf { it.number != null }
                    ?.let(onCardScanned)
            }
            .addOnCompleteListener {
                image.close()
                isInAnalysis.set(false)
            }
    }
}

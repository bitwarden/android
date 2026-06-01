@file:OmitFromCoverage

package com.bitwarden.ui.platform.feature.cardscanner.util

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.bitwarden.annotation.OmitFromCoverage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [CardTextAnalyzer] implementation that uses ML Kit Text Recognition to detect credit card
 * details from camera frames.
 *
 * Only used in the standard build flavor. The F-Droid flavor provides a no-op stub because
 * Google ML Kit is not permitted in F-Droid builds.
 *
 * The analyzer applies three layered defenses to prevent committing the wrong card data when
 * multiple cards are visible or a card is held off-axis:
 *  1. **Frame gating** — text outside the on-screen scan rectangle is discarded.
 *  2. **Orientation gating** — text whose baseline is more than ±10° from horizontal in display
 *     space is discarded (rejecting sideways and upside-down cards).
 *  3. **Temporal voting** — a PAN is only emitted once it has been observed in at least
 *     [TEMPORAL_VOTE_THRESHOLD] of the last [TEMPORAL_VOTE_WINDOW_SIZE] frames, and the emission
 *     is held until an expiration month has also been observed somewhere in that window.
 *
 * @property cardDataParser The parser used to extract card data from recognized text.
 */
@OmitFromCoverage
class CardTextAnalyzerImpl(
    private val cardDataParser: CardDataParser,
) : CardTextAnalyzer {

    private val isInAnalysis = AtomicBoolean(false)

    // Lazy so ML Kit is only touched once a scan begins, never during construction.
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val voteBuffer = PanVoteBuffer()

    private val expiryBuffer = ExpiryBuffer()

    override lateinit var onCardScanned: (CardScanData) -> Unit

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (!isInAnalysis.compareAndSet(false, true)) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            isInAnalysis.set(false)
            return
        }

        val rotationDegrees = image.imageInfo.rotationDegrees
        val rawImageWidth = mediaImage.width
        val rawImageHeight = mediaImage.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        recognizer.process(inputImage)
            .addOnSuccessListener { result ->
                val filteredText = filterScannedText(
                    recognized = result.toRecognizedText(),
                    rawImageWidth = rawImageWidth,
                    rawImageHeight = rawImageHeight,
                    rotationDegrees = rotationDegrees,
                )
                val parsed = filteredText
                    .takeIf { it.isNotEmpty() }
                    ?.let { cardDataParser.parseCardData(it) }

                voteAndMaybeEmit(parsed)
            }
            .addOnCompleteListener {
                image.close()
                isInAnalysis.set(false)
            }
    }

    /**
     * Records the latest frame's parse result in the temporal window and emits the parsed data
     * only when its PAN has been confirmed by [PanVoteBuffer] and an expiration month has been
     * observed somewhere in the same window via [ExpiryBuffer]. The PAN-confirming frame and the
     * expiry-bearing frame may differ, so the emission composes the confirmed PAN with the most
     * recent buffered expiry.
     */
    private fun voteAndMaybeEmit(parsed: ParsedCardFields?) {
        val confirmedPan = voteBuffer.record(parsed?.number)
        val latestExpiry = expiryBuffer.record(
            month = parsed?.expirationMonth,
            year = parsed?.expirationYear,
        )
        if (confirmedPan != null && latestExpiry != null) {
            onCardScanned(
                CardScanData(
                    number = confirmedPan,
                    expirationMonth = latestExpiry.month,
                    expirationYear = latestExpiry.year,
                    securityCode = parsed?.securityCode,
                ),
            )
        }
    }
}

/**
 * Adapts ML Kit's [Text] result into the analyzer's internal [RecognizedText] abstraction so the
 * geometric filtering logic can be unit-tested without mocking final ML Kit classes.
 */
private fun Text.toRecognizedText(): RecognizedText = object : RecognizedText {
    override val textBlocks: List<RecognizedTextBlock> =
        this@toRecognizedText.textBlocks.map { block ->
            object : RecognizedTextBlock {
                override val boundingBox: ImageRect? = block.boundingBox?.toImageRect()
                override val lines: List<RecognizedTextLine> = block.lines.map { line ->
                    object : RecognizedTextLine {
                        override val text: String = line.text
                        override val boundingBox: ImageRect? = line.boundingBox?.toImageRect()
                        override val cornerPoints: List<ImagePoint>? =
                            line.cornerPoints?.map { ImagePoint(x = it.x, y = it.y) }
                    }
                }
            }
        }
}

private fun Rect.toImageRect(): ImageRect =
    ImageRect(left = left, top = top, right = right, bottom = bottom)

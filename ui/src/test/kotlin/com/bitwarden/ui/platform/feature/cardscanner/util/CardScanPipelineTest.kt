package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * End-to-end tests for the geometric filter -> data parser -> vote buffer pipeline that
 * `CardTextAnalyzerImpl` runs on every camera frame. These tests do not exercise ML Kit itself,
 * but they validate the post-recognition logic that turns recognized text into emitted scans.
 */
class CardScanPipelineTest {

    private val parser = CardDataParserImpl()
    private val testImageSize = 1000

    @Test
    fun `pipeline ignores Luhn-valid PAN positioned outside the scan rectangle`() {
        // The PAN is in the top-left corner, far outside the centered scan rectangle.
        val recognized = singleHorizontalLine(
            text = "4111 1111 1111 1111",
            lineBounds = ImageRect(0, 0, 200, 30),
        )

        val emission = runFrame(recognized = recognized, voteBuffer = PanVoteBuffer())
        assertNull(emission)
    }

    @Test
    fun `pipeline ignores Luhn-valid PAN whose baseline is rotated more than ten degrees`() {
        // Sideways line: baseline drops straight down (90 degrees from horizontal).
        val sidewaysCorners = listOf(
            ImagePoint(500, 400),
            ImagePoint(500, 700),
            ImagePoint(550, 700),
            ImagePoint(550, 400),
        )
        val recognized = recognizedText(
            block(
                bounds = ImageRect(500, 400, 550, 700),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(500, 400, 550, 700),
                    corners = sidewaysCorners,
                ),
            ),
        )

        val emission = runFrame(recognized = recognized, voteBuffer = PanVoteBuffer())
        assertNull(emission)
    }

    @Test
    fun `pipeline does not emit a Luhn-valid PAN seen for the first time`() {
        val recognized = singleHorizontalLine(
            text = "4111 1111 1111 1111",
            lineBounds = ImageRect(400, 480, 700, 510),
        )

        val emission = runFrame(recognized = recognized, voteBuffer = PanVoteBuffer())
        assertNull(emission)
    }

    @Test
    fun `pipeline emits a Luhn-valid PAN seen in two frames`() {
        val recognized = singleHorizontalLine(
            text = "4111 1111 1111 1111",
            lineBounds = ImageRect(400, 480, 700, 510),
        )
        val voteBuffer = PanVoteBuffer()

        val firstEmission = runFrame(recognized = recognized, voteBuffer = voteBuffer)
        val secondEmission = runFrame(recognized = recognized, voteBuffer = voteBuffer)

        assertNull(firstEmission)
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = null,
                expirationYear = null,
                securityCode = null,
            ),
            secondEmission,
        )
    }

    @Test
    fun `pipeline does not emit a Luhn-valid PAN that briefly appears in a single frame`() {
        val voteBuffer = PanVoteBuffer()
        val withPan = singleHorizontalLine(
            text = "4111 1111 1111 1111",
            lineBounds = ImageRect(400, 480, 700, 510),
        )
        val withoutPan = recognizedText()

        assertNull(runFrame(recognized = withPan, voteBuffer = voteBuffer))
        assertNull(runFrame(recognized = withoutPan, voteBuffer = voteBuffer))
        assertNull(runFrame(recognized = withoutPan, voteBuffer = voteBuffer))
    }

    /**
     * Runs a single simulated frame through the same logical pipeline that `CardTextAnalyzerImpl`
     * applies and returns the emitted [CardScanData], or `null` if nothing should be emitted.
     */
    private fun runFrame(
        recognized: RecognizedText,
        voteBuffer: PanVoteBuffer,
    ): CardScanData? {
        val filteredText = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )
        val parsed = filteredText
            .takeIf { it.isNotEmpty() }
            ?.let { parser.parseCardData(it) }
            ?.takeIf { it.number != null }
        val confirmed = voteBuffer.record(pan = parsed?.number)
        return parsed?.takeIf { confirmed != null }
    }

    private fun singleHorizontalLine(
        text: String,
        lineBounds: ImageRect,
    ): RecognizedText = recognizedText(
        block(
            bounds = lineBounds,
            line(
                text = text,
                bounds = lineBounds,
                corners = listOf(
                    ImagePoint(lineBounds.left, lineBounds.top),
                    ImagePoint(lineBounds.right, lineBounds.top),
                    ImagePoint(lineBounds.right, lineBounds.bottom),
                    ImagePoint(lineBounds.left, lineBounds.bottom),
                ),
            ),
        ),
    )

    private fun line(
        text: String,
        bounds: ImageRect?,
        corners: List<ImagePoint>?,
    ): RecognizedTextLine = object : RecognizedTextLine {
        override val text: String = text
        override val boundingBox: ImageRect? = bounds
        override val cornerPoints: List<ImagePoint>? = corners
    }

    private fun block(
        bounds: ImageRect?,
        vararg lines: RecognizedTextLine,
    ): RecognizedTextBlock = object : RecognizedTextBlock {
        override val boundingBox: ImageRect? = bounds
        override val lines: List<RecognizedTextLine> = lines.toList()
    }

    private fun recognizedText(vararg blocks: RecognizedTextBlock): RecognizedText =
        object : RecognizedText {
            override val textBlocks: List<RecognizedTextBlock> = blocks.toList()
        }
}

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
        val recognized = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(0, 0, 200, 30),
        )

        val emission = runFrame(
            recognized = recognized,
            voteBuffer = PanVoteBuffer(),
            expiryBuffer = ExpiryBuffer(),
        )
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

        val emission = runFrame(
            recognized = recognized,
            voteBuffer = PanVoteBuffer(),
            expiryBuffer = ExpiryBuffer(),
        )
        assertNull(emission)
    }

    @Test
    fun `pipeline does not emit a Luhn-valid PAN seen for the first time`() {
        val recognized = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(400, 480, 700, 510),
            "12/28" at ImageRect(400, 540, 500, 570),
        )

        val emission = runFrame(
            recognized = recognized,
            voteBuffer = PanVoteBuffer(),
            expiryBuffer = ExpiryBuffer(),
        )
        assertNull(emission)
    }

    @Test
    fun `pipeline emits when both PAN and expiry are seen in two frames`() {
        val recognized = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(400, 480, 700, 510),
            "12/28" at ImageRect(400, 540, 500, 570),
        )
        val voteBuffer = PanVoteBuffer()
        val expiryBuffer = ExpiryBuffer()

        val firstEmission = runFrame(
            recognized = recognized,
            voteBuffer = voteBuffer,
            expiryBuffer = expiryBuffer,
        )
        val secondEmission = runFrame(
            recognized = recognized,
            voteBuffer = voteBuffer,
            expiryBuffer = expiryBuffer,
        )

        assertNull(firstEmission)
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2028",
                securityCode = null,
            ),
            secondEmission,
        )
    }

    @Test
    fun `pipeline does not emit when PAN is confirmed but expiry has never been seen`() {
        val voteBuffer = PanVoteBuffer()
        val expiryBuffer = ExpiryBuffer()
        val panOnly = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(400, 480, 700, 510),
        )

        assertNull(
            runFrame(recognized = panOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        assertNull(
            runFrame(recognized = panOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        assertNull(
            runFrame(recognized = panOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
    }

    @Test
    fun `pipeline emits using an expiry observed in an earlier frame within the window`() {
        val voteBuffer = PanVoteBuffer()
        val expiryBuffer = ExpiryBuffer()
        val expiryOnly = horizontalLines(
            "12/28" at ImageRect(400, 540, 500, 570),
        )
        val panOnly = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(400, 480, 700, 510),
        )

        // Frame 1: expiry observed, no PAN.
        assertNull(
            runFrame(recognized = expiryOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        // Frame 2: PAN seen for the first time, expiry not in this frame's parse.
        assertNull(
            runFrame(recognized = panOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        // Frame 3: PAN seen again — confirmed. Buffered expiry from frame 1 composes the emission.
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2028",
                securityCode = null,
            ),
            runFrame(recognized = panOnly, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
    }

    @Test
    fun `pipeline does not emit a Luhn-valid PAN that briefly appears in a single frame`() {
        val voteBuffer = PanVoteBuffer()
        val expiryBuffer = ExpiryBuffer()
        val withPan = horizontalLines(
            "4111 1111 1111 1111" at ImageRect(400, 480, 700, 510),
            "12/28" at ImageRect(400, 540, 500, 570),
        )
        val withoutPan = recognizedText()

        assertNull(
            runFrame(recognized = withPan, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        assertNull(
            runFrame(recognized = withoutPan, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
        assertNull(
            runFrame(recognized = withoutPan, voteBuffer = voteBuffer, expiryBuffer = expiryBuffer),
        )
    }

    /**
     * Runs a single simulated frame through the same logical pipeline that `CardTextAnalyzerImpl`
     * applies and returns the emitted [CardScanData], or `null` if nothing should be emitted.
     */
    private fun runFrame(
        recognized: RecognizedText,
        voteBuffer: PanVoteBuffer,
        expiryBuffer: ExpiryBuffer,
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
        val confirmedPan = voteBuffer.record(pan = parsed?.number)
        val latestExpiry = expiryBuffer.record(
            month = parsed?.expirationMonth,
            year = parsed?.expirationYear,
        )
        if (confirmedPan == null || latestExpiry == null) return null
        return CardScanData(
            number = confirmedPan,
            expirationMonth = latestExpiry.month,
            expirationYear = latestExpiry.year,
            securityCode = parsed?.securityCode,
        )
    }

    private infix fun String.at(bounds: ImageRect): Pair<String, ImageRect> = this to bounds

    private fun horizontalLines(vararg lines: Pair<String, ImageRect>): RecognizedText =
        recognizedText(
            *lines
                .map { (text, bounds) ->
                    block(
                        bounds = bounds,
                        line(
                            text = text,
                            bounds = bounds,
                            corners = listOf(
                                ImagePoint(bounds.left, bounds.top),
                                ImagePoint(bounds.right, bounds.top),
                                ImagePoint(bounds.right, bounds.bottom),
                                ImagePoint(bounds.left, bounds.bottom),
                            ),
                        ),
                    )
                }
                .toTypedArray(),
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

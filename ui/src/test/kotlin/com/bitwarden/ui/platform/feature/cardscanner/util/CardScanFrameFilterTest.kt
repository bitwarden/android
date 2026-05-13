package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class CardScanFrameFilterTest {

    /**
     * Square test image. With this size and zero rotation, the computed scan rectangle is roughly
     * (50, 216, 950, 783) in display coordinates.
     */
    private val testImageSize = 1000

    @Test
    fun `computeScanRect produces a centered card-shaped region for a square image`() {
        val rect = computeScanRect(imageWidth = 1000, imageHeight = 1000)

        // Centered horizontally and vertically (allow 1px slack for rounding).
        assertTrue(kotlin.math.abs((1000 - rect.right) - rect.left) <= 1)
        assertTrue(kotlin.math.abs((1000 - rect.bottom) - rect.top) <= 1)
        // Card aspect ratio is approximately 1.586.
        val ratio = rect.width.toFloat() / rect.height.toFloat()
        assertTrue(
            ratio in 1.55f..1.62f,
            "Expected card aspect ratio, got $ratio",
        )
    }

    @Test
    fun `filterScannedText returns the line text when the line is centered and horizontal`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 450, 700, 550),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 550),
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 550),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("4111 1111 1111 1111", result)
    }

    @Test
    fun `filterScannedText drops a line that lies entirely outside the scan rectangle`() {
        // A Luhn-valid PAN positioned in the top-left corner, well outside the centered scan
        // rectangle. The filter must drop it so the parser never sees it.
        val recognized = recognizedText(
            block(
                bounds = ImageRect(0, 0, 200, 100),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(0, 0, 200, 100),
                    corners = horizontalCorners(left = 0, top = 0, right = 200, bottom = 100),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText drops a line that only partially overlaps the scan rectangle`() {
        // The line straddles the left edge of the scan rect; we require full containment.
        val recognized = recognizedText(
            block(
                bounds = ImageRect(20, 450, 300, 550),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(20, 450, 300, 550),
                    corners = horizontalCorners(left = 20, top = 450, right = 300, bottom = 550),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText drops a line whose baseline is rotated more than ten degrees`() {
        // Line is centered in the scan rect but rotated ~30 degrees from horizontal.
        val tiltedCorners = listOf(
            ImagePoint(400, 500),
            ImagePoint(659, 650), // ~30 degree slope (atan2(150, 259) ~= 30)
            ImagePoint(640, 700),
            ImagePoint(380, 550),
        )
        val recognized = recognizedText(
            block(
                bounds = ImageRect(380, 500, 660, 700),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(380, 500, 660, 700),
                    corners = tiltedCorners,
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText drops a line rotated 90 degrees in image space`() {
        // The four corner points describe a vertical baseline (sideways card text).
        val sidewaysCorners = listOf(
            ImagePoint(500, 400),
            ImagePoint(500, 700), // top-left to top-right is straight down -> 90 degrees
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

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText accepts a line tilted within the ten degree tolerance`() {
        // ~5 degree tilt: dy/dx = tan(5 deg) ~= 0.0875. dx=200 -> dy~=17.
        val slightlyTiltedCorners = listOf(
            ImagePoint(400, 500),
            ImagePoint(600, 517),
            ImagePoint(600, 547),
            ImagePoint(400, 530),
        )
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 500, 600, 547),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 500, 600, 547),
                    corners = slightlyTiltedCorners,
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("4111 1111 1111 1111", result)
    }

    @Test
    fun `filterScannedText drops a line missing corner points`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 450, 700, 550),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 550),
                    corners = null,
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText reassembles multiple surviving lines separated by newlines`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 400, 700, 600),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 480),
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 480),
                ),
                line(
                    text = "12/25",
                    bounds = ImageRect(400, 500, 500, 530),
                    corners = horizontalCorners(left = 400, top = 500, right = 500, bottom = 530),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("4111 1111 1111 1111\n12/25", result)
    }

    @Test
    fun `filterScannedText keeps inside lines and drops outside lines from the same block`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(0, 0, 1000, 600),
                line(
                    text = "outside",
                    bounds = ImageRect(0, 0, 200, 100),
                    corners = horizontalCorners(left = 0, top = 0, right = 200, bottom = 100),
                ),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 480),
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 480),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("4111 1111 1111 1111", result)
    }

    @Test
    fun `filterScannedText respects rotation when judging line orientation`() {
        // Raw image is 1000 x 1000 and rotationDegrees=90 means the displayed image is rotated
        // 90 degrees clockwise from raw. A line that reads horizontally in display space, with
        // corner points reported in display reading order but expressed in raw image
        // coordinates, will have its raw corner points oriented vertically. After rotation, both
        // the bounding box and the baseline land in display space as horizontal — the line must
        // be accepted.
        val rawTopLeft = ImagePoint(490, 700)
        val rawTopRight = ImagePoint(490, 500)
        val rawBottomRight = ImagePoint(510, 500)
        val rawBottomLeft = ImagePoint(510, 700)

        val recognized = recognizedText(
            block(
                bounds = ImageRect(490, 500, 510, 700),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(490, 500, 510, 700),
                    corners = listOf(rawTopLeft, rawTopRight, rawBottomRight, rawBottomLeft),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 90,
        )

        assertEquals("4111 1111 1111 1111", result)
    }

    @Test
    fun `filterScannedText returns empty string when there are no text blocks`() {
        val recognized = recognizedText()

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText returns empty string for zero-sized images`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(0, 0, 0, 0),
                line(
                    text = "anything",
                    bounds = ImageRect(0, 0, 0, 0),
                    corners = horizontalCorners(left = 0, top = 0, right = 0, bottom = 0),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = 0,
            rawImageHeight = 0,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `isApproximatelyHorizontal accepts a perfectly horizontal line`() {
        val line = line(
            text = "x",
            bounds = ImageRect(0, 0, 100, 10),
            corners = horizontalCorners(left = 0, top = 0, right = 100, bottom = 10),
        )
        assertTrue(
            line.isApproximatelyHorizontal(
                rotationDegrees = 0,
                rawImageWidth = 1000,
                rawImageHeight = 1000,
            ),
        )
    }

    @Test
    fun `isApproximatelyHorizontal rejects a line missing corner points`() {
        val line = line(text = "x", bounds = ImageRect(0, 0, 100, 10), corners = null)
        assertFalse(
            line.isApproximatelyHorizontal(
                rotationDegrees = 0,
                rawImageWidth = 1000,
                rawImageHeight = 1000,
            ),
        )
    }

    @Test
    fun `isApproximatelyHorizontal rejects a line whose top corners coincide`() {
        // A degenerate line with a zero-length baseline cannot be classified — reject it.
        val degenerate = listOf(
            ImagePoint(500, 500),
            ImagePoint(500, 500),
            ImagePoint(500, 500),
            ImagePoint(500, 500),
        )
        val line = line(text = "x", bounds = ImageRect(500, 500, 500, 500), corners = degenerate)
        assertFalse(
            line.isApproximatelyHorizontal(
                rotationDegrees = 0,
                rawImageWidth = 1000,
                rawImageHeight = 1000,
            ),
        )
    }

    @Test
    fun `filterScannedText normalizes rotation values outside the canonical range`() {
        // 360 normalizes to 0 and should produce the same result as the centered-line zero-
        // rotation test, exercising the negative- and overflow-aware modulo arithmetic.
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 450, 700, 550),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 550),
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 550),
                ),
            ),
        )

        val resultPositive = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 360,
        )
        val resultNegative = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = -360,
        )

        assertEquals("4111 1111 1111 1111", resultPositive)
        assertEquals("4111 1111 1111 1111", resultNegative)
    }

    @Test
    fun `filterScannedText drops a block whose bounding box is null`() {
        val recognized = recognizedText(
            block(
                bounds = null,
                line(
                    text = "4111 1111 1111 1111",
                    bounds = ImageRect(400, 450, 700, 550),
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 550),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    @Test
    fun `filterScannedText drops a line whose bounding box is null`() {
        val recognized = recognizedText(
            block(
                bounds = ImageRect(400, 450, 700, 550),
                line(
                    text = "4111 1111 1111 1111",
                    bounds = null,
                    corners = horizontalCorners(left = 400, top = 450, right = 700, bottom = 550),
                ),
            ),
        )

        val result = filterScannedText(
            recognized = recognized,
            rawImageWidth = testImageSize,
            rawImageHeight = testImageSize,
            rotationDegrees = 0,
        )

        assertEquals("", result)
    }

    private fun horizontalCorners(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): List<ImagePoint> = listOf(
        ImagePoint(left, top),
        ImagePoint(right, top),
        ImagePoint(right, bottom),
        ImagePoint(left, bottom),
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

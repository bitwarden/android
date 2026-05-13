package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * A minimal abstraction over ML Kit's text recognition output that exposes only the geometry and
 * text needed for credit card scan filtering.
 *
 * Wrapping ML Kit's `Text` / `TextBlock` / `Line` types behind this interface keeps geometric
 * filtering logic pure JVM code that can be unit-tested without mocking final ML Kit classes or
 * pulling in `android.graphics` (whose stub `Rect`/`Point` types would yield zeros under the
 * `isReturnDefaultValues = true` JVM unit test configuration).
 */
interface RecognizedText {
    /**
     * The text blocks in the recognized image.
     */
    val textBlocks: List<RecognizedTextBlock>
}

/**
 * A single block of recognized text.
 */
interface RecognizedTextBlock {
    /**
     * The axis-aligned bounding box of the block in unrotated image coordinates, or `null` if
     * unavailable.
     */
    val boundingBox: ImageRect?

    /**
     * The lines that make up this block.
     */
    val lines: List<RecognizedTextLine>
}

/**
 * A single line of recognized text.
 */
interface RecognizedTextLine {
    /**
     * The recognized text for this line.
     */
    val text: String

    /**
     * The axis-aligned bounding box of the line in unrotated image coordinates, or `null` if
     * unavailable.
     */
    val boundingBox: ImageRect?

    /**
     * The four corner points of this line in unrotated image coordinates, ordered clockwise
     * starting from the top-left as the text reads, or `null` if unavailable.
     */
    val cornerPoints: List<ImagePoint>?
}

/**
 * An immutable, framework-free 2D point in image coordinates. Mirrors `android.graphics.Point`
 * but does not depend on the Android stub `android.jar` so the geometry filter can be exercised
 * in pure JVM unit tests.
 */
data class ImagePoint(val x: Int, val y: Int)

/**
 * An immutable, framework-free axis-aligned rectangle in image coordinates. Mirrors
 * `android.graphics.Rect`'s `(left, top, right, bottom)` semantics — `right` and `bottom` are
 * exclusive.
 */
data class ImageRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    /**
     * Returns `true` when [other] lies entirely within this rectangle (inclusive on left/top,
     * exclusive on right/bottom).
     */
    fun contains(other: ImageRect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    /**
     * Returns `true` when this rectangle and [other] overlap.
     */
    fun intersects(other: ImageRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom
}

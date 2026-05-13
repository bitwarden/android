package com.bitwarden.ui.platform.feature.cardscanner.util

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The fraction of the rotated image's longer edge that the scan rectangle's long edge occupies.
 *
 * The scan UI on `CardScanScreen` renders a rounded card-shaped overlay roughly centered in the
 * camera preview. Rather than plumb the exact composable bounds through the camera pipeline, we
 * approximate the on-screen scan rectangle as a centered region whose long edge is this fraction
 * of the rotated frame's longer edge. The fraction is intentionally generous so that a card held
 * within the visible overlay reliably falls inside the gate even when small misalignments occur,
 * while still excluding text rendered far away from the overlay (e.g. another card on the
 * counter, or app chrome above and below the preview).
 */
private const val SCAN_RECT_LONG_EDGE_FRACTION_OF_LONG_EDGE = 0.9f

/**
 * Standard credit card aspect ratio (long edge / short edge), per ISO/IEC 7810 ID-1.
 */
private const val CARD_ASPECT_RATIO = 1.586f

/**
 * The maximum baseline deviation, in degrees, that a recognized line may have from horizontal in
 * display space before being rejected. Cards must be approximately upright; sideways or tilted
 * cards are rejected before parsing.
 */
internal const val MAX_BASELINE_ANGLE_DEGREES: Double = 10.0

/**
 * Number of corner points reported by ML Kit for each recognized line.
 */
private const val LINE_CORNER_POINT_COUNT: Int = 4

/**
 * Full rotation in degrees, used to normalize incoming rotation values into the [0, 360) range.
 */
private const val FULL_ROTATION_DEGREES: Int = 360

/**
 * Half rotation in degrees. A 90 or 270 degree rotation swaps raw image dimensions; rotations
 * that are even multiples of [HALF_ROTATION_DEGREES] do not.
 */
private const val HALF_ROTATION_DEGREES: Int = 180

/**
 * Quarter rotation in degrees (clockwise).
 */
private const val QUARTER_ROTATION_DEGREES: Int = 90

/**
 * Three-quarter rotation in degrees (clockwise).
 */
private const val THREE_QUARTER_ROTATION_DEGREES: Int = 270

/**
 * Computes the approximated scan rectangle in display-space coordinates (i.e. coordinates after
 * the supplied rotation has been applied to the raw image).
 *
 * The rectangle is centered, has its long edge oriented along the image's longer axis, and is
 * sized to a credit card aspect ratio. If the card-aspect rectangle would be taller than the
 * image's short edge, the rectangle is clamped to the short edge to avoid producing a region
 * that extends past the visible frame.
 */
internal fun computeScanRect(
    imageWidth: Int,
    imageHeight: Int,
): ImageRect {
    val shortEdge = min(imageWidth, imageHeight).toFloat()
    val longEdge = max(imageWidth, imageHeight).toFloat()
    val isLandscape = imageWidth >= imageHeight

    val rectLong = (longEdge * SCAN_RECT_LONG_EDGE_FRACTION_OF_LONG_EDGE)
        .coerceAtMost(shortEdge * CARD_ASPECT_RATIO)
    val rectShort = rectLong / CARD_ASPECT_RATIO

    val rectWidth = if (isLandscape) rectLong else rectShort
    val rectHeight = if (isLandscape) rectShort else rectLong

    val left = ((imageWidth - rectWidth) / 2f).roundToInt()
    val top = ((imageHeight - rectHeight) / 2f).roundToInt()
    return ImageRect(
        left = left,
        top = top,
        right = left + rectWidth.roundToInt(),
        bottom = top + rectHeight.roundToInt(),
    )
}

/**
 * Rotates an [ImagePoint] from raw image space into display space given the supplied
 * [rotationDegrees] (one of 0, 90, 180, 270) and the raw image dimensions.
 */
internal fun rotatePoint(
    point: ImagePoint,
    rotationDegrees: Int,
    rawImageWidth: Int,
    rawImageHeight: Int,
): ImagePoint = when (rotationDegrees.normalizeRotation()) {
    QUARTER_ROTATION_DEGREES -> ImagePoint(x = rawImageHeight - point.y, y = point.x)
    HALF_ROTATION_DEGREES -> ImagePoint(x = rawImageWidth - point.x, y = rawImageHeight - point.y)
    THREE_QUARTER_ROTATION_DEGREES -> ImagePoint(x = point.y, y = rawImageWidth - point.x)
    else -> point
}

/**
 * Rotates an [ImageRect] from raw image space into display space.
 */
internal fun rotateRect(
    rect: ImageRect,
    rotationDegrees: Int,
    rawImageWidth: Int,
    rawImageHeight: Int,
): ImageRect {
    val corners = listOf(
        ImagePoint(rect.left, rect.top),
        ImagePoint(rect.right, rect.top),
        ImagePoint(rect.right, rect.bottom),
        ImagePoint(rect.left, rect.bottom),
    ).map { rotatePoint(it, rotationDegrees, rawImageWidth, rawImageHeight) }
    val xs = corners.map { it.x }
    val ys = corners.map { it.y }
    return ImageRect(left = xs.min(), top = ys.min(), right = xs.max(), bottom = ys.max())
}

/**
 * Returns `true` if the line's baseline (from top-left to top-right corner, in display space)
 * deviates by no more than [MAX_BASELINE_ANGLE_DEGREES] from horizontal.
 *
 * Lines without exactly four corner points are conservatively rejected — we cannot verify the
 * orientation of an unbounded line and would rather drop it than risk parsing a sideways card.
 */
internal fun RecognizedTextLine.isApproximatelyHorizontal(
    rotationDegrees: Int,
    rawImageWidth: Int,
    rawImageHeight: Int,
): Boolean {
    val corners = cornerPoints?.takeIf { it.size == LINE_CORNER_POINT_COUNT } ?: return false
    val topLeft = rotatePoint(corners[0], rotationDegrees, rawImageWidth, rawImageHeight)
    val topRight = rotatePoint(corners[1], rotationDegrees, rawImageWidth, rawImageHeight)
    val dx = (topRight.x - topLeft.x).toDouble()
    val dy = (topRight.y - topLeft.y).toDouble()
    if (dx == 0.0 && dy == 0.0) return false
    val angleDegrees = Math.toDegrees(atan2(dy, dx))
    return abs(angleDegrees) <= MAX_BASELINE_ANGLE_DEGREES
}

/**
 * Filters [recognized] text down to lines whose bounding box lies inside the on-screen scan
 * rectangle and whose baseline is approximately horizontal in display space, then reassembles
 * the surviving lines into a single newline-separated string.
 *
 * Returns an empty string if no lines survive filtering — callers should treat this as "no card
 * text found in this frame" rather than passing it to the parser.
 *
 * @param recognized The text recognized in the raw camera image.
 * @param rawImageWidth The width of the raw camera image (before rotation is applied).
 * @param rawImageHeight The height of the raw camera image (before rotation is applied).
 * @param rotationDegrees The rotation that should be applied to display the image upright.
 */
fun filterScannedText(
    recognized: RecognizedText,
    rawImageWidth: Int,
    rawImageHeight: Int,
    rotationDegrees: Int,
): String {
    if (rawImageWidth <= 0 || rawImageHeight <= 0) return ""

    val normalizedRotation = rotationDegrees.normalizeRotation()
    val swapDimensions = normalizedRotation % HALF_ROTATION_DEGREES != 0
    val displayWidth = if (swapDimensions) rawImageHeight else rawImageWidth
    val displayHeight = if (swapDimensions) rawImageWidth else rawImageHeight
    val scanRect = computeScanRect(imageWidth = displayWidth, imageHeight = displayHeight)

    return recognized
        .textBlocks
        .filter { block ->
            block.intersectsScanRect(
                scanRect = scanRect,
                rotationDegrees = normalizedRotation,
                rawImageWidth = rawImageWidth,
                rawImageHeight = rawImageHeight,
            )
        }
        .flatMap { block -> block.lines }
        .filter { line ->
            line.isInsideScanRect(
                scanRect = scanRect,
                rotationDegrees = normalizedRotation,
                rawImageWidth = rawImageWidth,
                rawImageHeight = rawImageHeight,
            )
        }
        .filter { line ->
            line.isApproximatelyHorizontal(
                rotationDegrees = normalizedRotation,
                rawImageWidth = rawImageWidth,
                rawImageHeight = rawImageHeight,
            )
        }
        .joinToString(separator = "\n") { it.text }
}

/**
 * Returns `true` when the rotated bounding box of this block overlaps [scanRect]. Blocks with
 * no bounding box are conservatively excluded.
 */
private fun RecognizedTextBlock.intersectsScanRect(
    scanRect: ImageRect,
    rotationDegrees: Int,
    rawImageWidth: Int,
    rawImageHeight: Int,
): Boolean {
    val box = boundingBox ?: return false
    val rotated = rotateRect(box, rotationDegrees, rawImageWidth, rawImageHeight)
    return scanRect.intersects(rotated)
}

/**
 * Returns `true` when the rotated bounding box of this line lies entirely within [scanRect].
 * Lines with no bounding box are conservatively excluded.
 */
private fun RecognizedTextLine.isInsideScanRect(
    scanRect: ImageRect,
    rotationDegrees: Int,
    rawImageWidth: Int,
    rawImageHeight: Int,
): Boolean {
    val box = boundingBox ?: return false
    val rotated = rotateRect(box, rotationDegrees, rawImageWidth, rawImageHeight)
    return scanRect.contains(rotated)
}

/**
 * Normalizes a rotation in degrees to the range `[0, 360)`.
 */
private fun Int.normalizeRotation(): Int =
    ((this % FULL_ROTATION_DEGREES) + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES

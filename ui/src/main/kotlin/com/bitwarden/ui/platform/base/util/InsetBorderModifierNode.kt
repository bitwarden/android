package com.bitwarden.ui.platform.base.util

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * The [ModifierNodeElement] that creates and updates an [InsetBorderDrawModifierNode].
 *
 * [alpha] takes part in the equality of this element so that the node always holds the most recent
 * accessor, but supplying a new one never invalidates the cached outline.
 */
internal data class InsetBorderModifierNodeElement(
    private val border: BorderStroke,
    private val shape: Shape,
    private val inset: Dp,
    private val alpha: () -> Float,
) : ModifierNodeElement<InsetBorderDrawModifierNode>() {
    override fun create(): InsetBorderDrawModifierNode = InsetBorderDrawModifierNode(
        border = border,
        shape = shape,
        inset = inset,
        alpha = alpha,
    )

    override fun update(node: InsetBorderDrawModifierNode) {
        node.updateAlpha(alpha = alpha)
        node.updateBorder(border = border, shape = shape, inset = inset)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "insetBorder"
        properties["border"] = border
        properties["shape"] = shape
        properties["inset"] = inset
    }
}

/**
 * A [DrawModifierNode] that draws [border] as a true parallel offset of [shape], sitting [inset]
 * inside the content it decorates.
 *
 * Building the offset outline is costly enough to be worth holding on to, so it is rebuilt only
 * when the border, shape, inset, size, layout direction, or density actually change — never merely
 * because the composable that applied the modifier recomposed. [alpha] is read during the draw
 * phase, so an animated value repaints the border without recomposing, and a fully transparent
 * border builds no outline at all.
 */
internal class InsetBorderDrawModifierNode(
    private var border: BorderStroke,
    private var shape: Shape,
    private var inset: Dp,
    private var alpha: () -> Float,
) : Modifier.Node(), DrawModifierNode {
    private var cachedOutline: Outline? = null
    private var isOutlineCached: Boolean = false
    private var cachedSize: Size = Size.Zero
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity: Float = 0f

    /**
     * Invalidation is driven by [updateAlpha] and [updateBorder] rather than by every update of the
     * owning element.
     */
    override val shouldAutoInvalidate: Boolean get() = false

    /**
     * Applies the [alpha] accessor, repainting only when a different accessor is supplied.
     */
    fun updateAlpha(alpha: () -> Float) {
        if (this.alpha === alpha) return
        this.alpha = alpha
        invalidateDraw()
    }

    /**
     * Applies the [border], [shape], and [inset], discarding the cached outline and repainting only
     * when one of them has actually changed.
     */
    fun updateBorder(border: BorderStroke, shape: Shape, inset: Dp) {
        if (this.border == border && this.shape == shape && this.inset == inset) return
        this.border = border
        this.shape = shape
        this.inset = inset
        isOutlineCached = false
        cachedOutline = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val currentAlpha = alpha()
        if (currentAlpha <= 0f || this.size.isEmpty()) return
        val strokeWidth = border.width.toPx()
        val outline = obtainOutline(strokeWidth = strokeWidth) ?: return
        drawOutline(
            outline = outline,
            brush = border.brush,
            style = Stroke(width = strokeWidth),
            alpha = currentAlpha,
        )
    }

    /**
     * Returns the offset outline for the current draw environment, reusing the cached one whenever
     * nothing it depends on has changed.
     */
    private fun ContentDrawScope.obtainOutline(strokeWidth: Float): Outline? {
        val isCacheUsable = isOutlineCached &&
            cachedSize == this.size &&
            cachedLayoutDirection == this.layoutDirection &&
            cachedDensity == this.density
        if (isCacheUsable) return cachedOutline
        cachedSize = this.size
        cachedLayoutDirection = this.layoutDirection
        cachedDensity = this.density
        isOutlineCached = true
        cachedOutline = shape
            .createOutline(
                size = this.size,
                layoutDirection = this.layoutDirection,
                density = this,
            )
            .insetBy(inset = inset.toPx() + (strokeWidth / 2f))
        return cachedOutline
    }
}

/**
 * Returns the [Outline] offset inwards by [inset] pixels, or `null` when the [inset] consumes the
 * outline entirely or the offset cannot be computed.
 *
 * Rectangles and round rects are offset analytically. Any other outline is a path, which is offset
 * by subtracting the band swept by a stroke of width `2 * inset` centered on its boundary, leaving
 * exactly the region more than [inset] pixels inside the original. Elliptical round rect corners
 * are approximated by shortening both radii, which is exact only for circular corners.
 */
private fun Outline.insetBy(
    inset: Float,
): Outline? = when (this) {
    is Outline.Rectangle -> {
        this
            .rect
            .deflate(delta = inset)
            .takeUnless { it.isEmpty }
            ?.let { Outline.Rectangle(rect = it) }
    }

    is Outline.Rounded -> {
        RoundRect(
            left = this.roundRect.left + inset,
            top = this.roundRect.top + inset,
            right = this.roundRect.right - inset,
            bottom = this.roundRect.bottom - inset,
            topLeftCornerRadius = this.roundRect.topLeftCornerRadius.insetBy(inset = inset),
            topRightCornerRadius = this.roundRect.topRightCornerRadius.insetBy(inset = inset),
            bottomRightCornerRadius = this.roundRect.bottomRightCornerRadius.insetBy(inset = inset),
            bottomLeftCornerRadius = this.roundRect.bottomLeftCornerRadius.insetBy(inset = inset),
        )
            .takeUnless { it.isEmpty }
            ?.let { Outline.Rounded(roundRect = it) }
    }

    is Outline.Generic -> {
        this.path.insetBy(inset = inset)?.let { Outline.Generic(path = it) }
    }
}

private fun CornerRadius.insetBy(
    inset: Float,
): CornerRadius = CornerRadius(
    x = (this.x - inset).coerceAtLeast(minimumValue = 0f),
    y = (this.y - inset).coerceAtLeast(minimumValue = 0f),
)

/**
 * Returns the [Path] offset inwards by [inset] pixels, or `null` when the offset leaves nothing to
 * draw. The band swept by a stroke of width `2 * inset` along the path covers every point within
 * [inset] pixels of the boundary, so removing it from the filled path leaves the inward offset.
 */
private fun Path.insetBy(inset: Float): Path? {
    if (inset <= 0f) return this
    val boundaryBand = Path()
    Paint()
        .apply {
            style = Paint.Style.STROKE
            strokeWidth = inset * 2f
            strokeJoin = Paint.Join.MITER
        }
        .getFillPath(this.asAndroidPath(), boundaryBand.asAndroidPath())
    val insetPath = Path()
    val isSuccessful = insetPath.op(
        path1 = this,
        path2 = boundaryBand,
        operation = PathOperation.Difference,
    )
    return insetPath.takeIf { isSuccessful && !it.isEmpty }
}

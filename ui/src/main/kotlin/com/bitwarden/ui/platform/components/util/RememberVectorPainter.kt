package com.bitwarden.ui.platform.components.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme

/**
 * Returns a [VectorPainter] built from the given [id] to circumvent issues with painter resources
 * recomposing unnecessarily.
 */
@Composable
fun rememberVectorPainter(
    @DrawableRes id: Int,
): VectorPainter = rememberVectorPainter(
    image = multiTonalVectorResource(id = id),
)

/**
 * Creates a [ImageVector] and updates the path colors to match the current theme.
 */
@Composable
private fun multiTonalVectorResource(
    @DrawableRes id: Int,
): ImageVector {
    val originalImage = ImageVector.vectorResource(id = id)
    return ImageVector
        .Builder(
            name = originalImage.name,
            defaultWidth = originalImage.defaultWidth,
            defaultHeight = originalImage.defaultHeight,
            viewportWidth = originalImage.viewportWidth,
            viewportHeight = originalImage.viewportHeight,
            tintColor = originalImage.tintColor,
            tintBlendMode = originalImage.tintBlendMode,
            autoMirror = originalImage.autoMirror,
        )
        .cloneVectorGroupWithUpdatedColors(
            vectorGroup = originalImage.root,
            colors = BitwardenTheme.colorScheme,
        )
        .build()
}

/**
 * Copies the given [vectorGroup] into the builder while updating the the path colors with the
 * provided [colors].
 */
private fun ImageVector.Builder.cloneVectorGroupWithUpdatedColors(
    vectorGroup: VectorGroup,
    colors: BitwardenColorScheme,
): ImageVector.Builder {
    vectorGroup.iterator().forEach { vectorNode ->
        when (vectorNode) {
            is VectorGroup -> {
                addGroup(
                    name = vectorNode.name,
                    rotate = vectorNode.rotation,
                    pivotX = vectorNode.pivotX,
                    pivotY = vectorNode.pivotY,
                    scaleX = vectorNode.scaleX,
                    scaleY = vectorNode.scaleY,
                    translationX = vectorNode.translationX,
                    translationY = vectorNode.translationY,
                    clipPathData = vectorNode.clipPathData,
                )
                cloneVectorGroupWithUpdatedColors(vectorGroup = vectorNode, colors = colors)
                clearGroup()
            }

            is VectorPath -> {
                addPath(
                    pathData = vectorNode.pathData,
                    pathFillType = vectorNode.pathFillType,
                    name = vectorNode.name,
                    fill = when (vectorNode.name) {
                        "outline" -> SolidColor(colors.illustration.outline)
                        "primary" -> SolidColor(colors.illustration.backgroundPrimary)
                        "secondary" -> SolidColor(colors.illustration.backgroundSecondary)
                        "tertiary" -> SolidColor(colors.illustration.backgroundTertiary)
                        "accent" -> SolidColor(colors.illustration.accent)
                        "logo" -> SolidColor(colors.illustration.logo)
                        "navigation" -> SolidColor(colors.icon.secondary)
                        "navigationActiveAccent" -> SolidColor(colors.icon.navActiveAccent)
                        else -> vectorNode.fill
                    },
                    fillAlpha = vectorNode.fillAlpha,
                    stroke = vectorNode.stroke,
                    strokeAlpha = vectorNode.strokeAlpha,
                    strokeLineWidth = vectorNode.strokeLineWidth,
                    strokeLineCap = vectorNode.strokeLineCap,
                    strokeLineJoin = vectorNode.strokeLineJoin,
                    strokeLineMiter = vectorNode.strokeLineMiter,
                    trimPathStart = vectorNode.trimPathStart,
                    trimPathEnd = vectorNode.trimPathEnd,
                    trimPathOffset = vectorNode.trimPathOffset,
                )
            }
        }
    }
    return this
}

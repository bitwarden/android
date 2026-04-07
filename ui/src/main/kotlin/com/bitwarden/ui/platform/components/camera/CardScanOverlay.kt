package com.bitwarden.ui.platform.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme

private const val CARD_ASPECT_RATIO = 1.586f

/**
 * A rectangular overlay sized to a credit card aspect ratio (~1.586:1).
 *
 * @param overlayWidth The width of the card overlay.
 * @param modifier The [Modifier] for this composable.
 * @param color The color of the overlay border.
 * @param strokeWidth The stroke width of the overlay border.
 */
@Composable
fun CardScanOverlay(
    overlayWidth: Dp,
    modifier: Modifier = Modifier,
    color: Color = BitwardenTheme.colorScheme.text.primary,
    strokeWidth: Dp = 3.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        CardScanOverlayCanvas(
            color = color,
            strokeWidth = strokeWidth,
            modifier = Modifier
                .padding(all = 8.dp)
                .width(overlayWidth)
                .aspectRatio(CARD_ASPECT_RATIO),
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun CardScanOverlayCanvas(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val cornerRadiusPx = 12.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = Size(
                width = size.width - strokeWidthPx,
                height = size.height - strokeWidthPx,
            ),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidthPx),
        )
    }
}

package com.bitwarden.ui.platform.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The UI for the QR code square overlay that is drawn onto the screen.
 */
@Composable
fun QrCodeSquare(
    squareOutlineSize: Dp,
    modifier: Modifier = Modifier,
    color: Color = BitwardenTheme.colorScheme.text.primary,
    strokeWidth: Dp = 3.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        QrCodeSquareCanvas(
            color = color,
            strokeWidth = strokeWidth,
            modifier = Modifier
                .padding(all = 8.dp)
                .size(size = squareOutlineSize),
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun QrCodeSquareCanvas(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val squareSize = size.width
        val strokeOffset = strokeWidthPx / 2
        val sideLength = (1f / 6) * squareSize
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.apply {
                // Draw upper top left.
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = strokeOffset),
                    end = Offset(x = sideLength, y = strokeOffset),
                    strokeWidth = strokeWidthPx,
                )
                // Draw lower top left.
                drawLine(
                    color = color,
                    start = Offset(x = strokeOffset, y = strokeOffset),
                    end = Offset(x = strokeOffset, y = sideLength),
                    strokeWidth = strokeWidthPx,
                )
                // Draw upper top right.
                drawLine(
                    color = color,
                    start = Offset(x = squareSize - sideLength, y = strokeOffset),
                    end = Offset(x = squareSize - strokeOffset, y = strokeOffset),
                    strokeWidth = strokeWidthPx,
                )
                // Draw lower top right.
                drawLine(
                    color = color,
                    start = Offset(x = squareSize - strokeOffset, y = 0f),
                    end = Offset(x = squareSize - strokeOffset, y = sideLength),
                    strokeWidth = strokeWidthPx,
                )
                // Draw upper bottom right.
                drawLine(
                    color = color,
                    start = Offset(x = squareSize - strokeOffset, y = squareSize),
                    end = Offset(x = squareSize - strokeOffset, y = squareSize - sideLength),
                    strokeWidth = strokeWidthPx,
                )
                // Draw lower bottom right.
                drawLine(
                    color = color,
                    start = Offset(x = squareSize - strokeOffset, y = squareSize - strokeOffset),
                    end = Offset(x = squareSize - sideLength, y = squareSize - strokeOffset),
                    strokeWidth = strokeWidthPx,
                )
                // Draw upper bottom left.
                drawLine(
                    color = color,
                    start = Offset(x = strokeOffset, y = squareSize),
                    end = Offset(x = strokeOffset, y = squareSize - sideLength),
                    strokeWidth = strokeWidthPx,
                )
                // Draw lower bottom left.
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = squareSize - strokeOffset),
                    end = Offset(x = sideLength, y = squareSize - strokeOffset),
                    strokeWidth = strokeWidthPx,
                )
            }
        }
    }
}

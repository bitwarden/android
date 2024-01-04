package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Adds a performance-optimized background color specified by the given [topAppBarScrollBehavior]
 * and its current scroll state.
 */
@OmitFromCoverage
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun Modifier.scrolledContainerBackground(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
): Modifier {
    val expandedColor = MaterialTheme.colorScheme.surface
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainer
    return this then drawBehind {
        drawRect(
            color = topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )
    }
}

/**
 * This is a [Modifier] extension for drawing a divider at the bottom of the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.bottomDivider(
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
    enabled: Boolean = true,
): Modifier = drawWithCache {
    onDrawWithContent {
        drawContent()
        if (enabled) {
            drawLine(
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(
                    x = paddingStart.toPx(),
                    y = size.height - thickness.toPx() / 2,
                ),
                end = Offset(
                    x = size.width - paddingEnd.toPx(),
                    y = size.height - thickness.toPx() / 2,
                ),
            )
        }
    }
}

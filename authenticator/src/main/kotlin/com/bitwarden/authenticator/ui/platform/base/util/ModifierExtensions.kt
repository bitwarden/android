package com.bitwarden.authenticator.ui.platform.base.util

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.data.platform.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.platform.util.isPortrait

/**
 * This is a [Modifier] extension for drawing a divider at the bottom of the composable.
 */
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

/**
 * This is a [Modifier] extension for mirroring the contents of a composable when the layout
 * direction is set to [LayoutDirection.Rtl]. Primarily used for directional icons, such as the
 * up button and chevrons.
 */
@Stable
@Composable
fun Modifier.mirrorIfRtl(): Modifier =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        scale(scaleX = -1f, scaleY = 1f)
    } else {
        this
    }

/**
 * This is a [Modifier] extension for ensuring that the content uses the standard horizontal margin.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.standardHorizontalMargin(
    portrait: Dp = 16.dp,
    landscape: Dp = 48.dp,
): Modifier {
    val config = LocalConfiguration.current
    return this.padding(horizontal = if (config.isPortrait) portrait else landscape)
}

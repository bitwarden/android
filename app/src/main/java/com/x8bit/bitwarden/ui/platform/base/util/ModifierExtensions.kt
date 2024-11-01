package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.isPortrait

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
    val expandedColor = BitwardenTheme.colorScheme.background.secondary
    val collapsedColor = BitwardenTheme.colorScheme.background.secondary
    return CombinedModifier(
        outer = this,
        inner = drawBehind {
            drawRect(
                color = topAppBarScrollBehavior.toScrolledContainerColor(
                    expandedColor = expandedColor,
                    collapsedColor = collapsedColor,
                ),
            )
        },
    )
}

/**
 * Adds a bottom divider specified by the given [topAppBarScrollBehavior] and its current scroll
 * state.
 */
@OmitFromCoverage
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun Modifier.scrolledContainerBottomDivider(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    enabled: Boolean = true,
): Modifier =
    this.bottomDivider(
        alpha = topAppBarScrollBehavior.toScrolledContainerDividerAlpha(),
        enabled = enabled,
    )

/**
 * This is a [Modifier] extension for drawing a divider at the bottom of the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.bottomDivider(
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    thickness: Dp = (0.5).dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
    enabled: Boolean = true,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    onDrawWithContent {
        drawContent()
        if (enabled) {
            drawLine(
                alpha = alpha,
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
@OmitFromCoverage
@Stable
@Composable
fun Modifier.mirrorIfRtl(): Modifier =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        scale(scaleX = -1f, scaleY = 1f)
    } else {
        this
    }

/**
 * This is a [Modifier] extension for ensuring that the tab button navigates properly when using
 * a physical keyboard.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.tabNavigation(): Modifier {
    val focusManager = LocalFocusManager.current
    return onPreviewKeyEvent { keyEvent ->
        if (keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown) {
            focusManager.moveFocus(
                if (keyEvent.isShiftPressed) {
                    FocusDirection.Previous
                } else {
                    FocusDirection.Next
                },
            )
            true
        } else {
            false
        }
    }
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

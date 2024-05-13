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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

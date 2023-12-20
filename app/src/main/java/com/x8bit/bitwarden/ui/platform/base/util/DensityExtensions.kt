package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

/**
 * A function for converting pixels to [Dp] within a composable function.
 */
@Composable
fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

/**
 * A function for converting pixels to [Dp].
 */
fun Int.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }

/**
 * A function for converting [IntSize] pixels into [DpSize].
 */
fun IntSize.toDpSize(density: Density): DpSize = with(density) {
    DpSize(
        width = width.toDp(),
        height = height.toDp(),
    )
}

/**
 * A function for converting [Dp] to pixels within a composable function.
 */
@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

/**
 * Converts a [Dp] value to [TextUnit] with [TextUnitType.Sp] as its type.
 *
 * This allows for easier conversion between density-independent pixels (dp) and
 * scalable pixels (sp) when setting text sizes in Compose. For example, a dp value
 * representing a size can be directly used for text styling.
 */
@Composable
fun Dp.toUnscaledTextUnit(): TextUnit {
    val scalingFactor = LocalConfiguration.current.fontScale
    return TextUnit(value / scalingFactor, TextUnitType.Sp)
}

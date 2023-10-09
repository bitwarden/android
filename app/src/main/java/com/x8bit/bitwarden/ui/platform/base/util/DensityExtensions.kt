package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

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

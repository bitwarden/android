package com.x8bit.bitwarden.authenticator.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * A function for converting [Dp] to pixels within a composable function.
 */
@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }

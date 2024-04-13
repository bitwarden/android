package com.x8bit.bitwarden.authenticator.ui.platform.base.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Compares the top, bottom, start, and end values to another [PaddingValues] and returns a new
 * 'PaddingValues' using the maximum values of each property respectively.
 *
 * @param other The other values to compare against.
 */
fun PaddingValues.max(
    other: PaddingValues,
    direction: LayoutDirection,
): PaddingValues = PaddingValues(
    top = maxOf(calculateTopPadding(), other.calculateTopPadding()),
    bottom = maxOf(calculateBottomPadding(), other.calculateBottomPadding()),
    start = maxOf(calculateStartPadding(direction), other.calculateStartPadding(direction)),
    end = maxOf(calculateEndPadding(direction), other.calculateEndPadding(direction)),
)

/**
 * Compares the top, bottom, start, and end values to a [WindowInsets] and returns a new
 * 'PaddingValues' using the maximum values of each property respectively.
 *
 * @param windowInsets The [WindowInsets] to compare against.
 */
@Composable
fun PaddingValues.max(
    windowInsets: WindowInsets,
): PaddingValues = max(windowInsets.asPaddingValues())

/**
 * Compares the top, bottom, start, and end values to another [PaddingValues] and returns a new
 * 'PaddingValues' using the maximum values of each property respectively.
 *
 * @param other The other [PaddingValues] to compare against.
 */
@Composable
fun PaddingValues.max(
    other: PaddingValues,
): PaddingValues = max(other, LocalLayoutDirection.current)

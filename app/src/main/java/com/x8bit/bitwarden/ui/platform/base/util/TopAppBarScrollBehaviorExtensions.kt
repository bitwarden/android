package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp

/**
 * Returns the correct color for a scrolled container based on the given [TopAppBarScrollBehavior]
 * and target [expandedColor] / [collapsedColor].
 */
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarScrollBehavior.toScrolledContainerColor(
    expandedColor: Color,
    collapsedColor: Color,
): Color {
    val progressFraction = if (this.isPinned) {
        this.state.overlappedFraction
    } else {
        this.state.collapsedFraction
    }
    return lerp(
        start = expandedColor,
        stop = collapsedColor,
        // The easing function here matches what is currently in TopAppBarColors.containerColor and
        // is necessary to match to the app bar color through the full range of motion.
        fraction = FastOutLinearInEasing.transform(progressFraction),
    )
}

/**
 * Returns the correct alpha, as a [Float], for a containers alpha based on the given
 * [TopAppBarScrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarScrollBehavior.toScrolledContainerDividerAlpha(): Float {
    val progressFraction = if (this.isPinned) {
        this.state.overlappedFraction
    } else {
        this.state.collapsedFraction
    }
    return lerp(
        start = 0f,
        stop = 1f,
        // The easing function here matches what is currently in TopAppBarColors.containerColor
        fraction = FastOutLinearInEasing.transform(fraction = progressFraction),
    )
}

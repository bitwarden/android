package com.bitwarden.ui.platform.components.segment.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val TWEEN_DURATION_MS: Int = 300

/**
 * A standard [ContentTransform] to be used when animating to different content states of a
 * segmented control indicated by the [Enum].
 */
fun <T : Enum<T>> AnimatedContentTransitionScope<T>.segmentedContentTransform(): ContentTransform {
    // Slide in from right if moving forward, from left if moving backward
    return if (targetState.ordinal > initialState.ordinal) {
        (slideInHorizontally { width -> width } + fadeIn(tween(TWEEN_DURATION_MS))) togetherWith
            slideOutHorizontally { width -> -width } + fadeOut(tween(TWEEN_DURATION_MS))
    } else {
        (slideInHorizontally { width -> -width } + fadeIn(tween(TWEEN_DURATION_MS))) togetherWith
            slideOutHorizontally { width -> width } + fadeOut(tween(TWEEN_DURATION_MS))
    }
}

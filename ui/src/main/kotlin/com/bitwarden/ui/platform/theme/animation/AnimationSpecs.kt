package com.bitwarden.ui.platform.theme.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * A custom easing to ensure elements exit slowly.
 */
private val SlowOutSlowInEasing: Easing = CubicBezierEasing(a = 0.8f, b = 0.0f, c = 0.2f, d = 1.0f)

/**
 * A custom [AnimationSpec] to be used for consistent focus state animations.
 */
internal fun <T : Any> focusAnimationSpec(): AnimationSpec<T> = tween(
    durationMillis = 200,
    easing = SlowOutSlowInEasing,
)

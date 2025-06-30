package com.bitwarden.ui.platform.components.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An animation API that tries to work like AnimateVisibility, animating in when the content is
 * present and animating out when null.
 *
 * @param targetState The state the animation should animate towards.
 * @param modifier The [Modifier] for the component.
 * @param enter The [EnterTransition] defining how the content animates in.
 * @param exit The [ExitTransition] defining how the content animates out.
 * @param label An optional parameter to differentiate from other animations.
 * @param content The lambda that for the UI.
 */
@Composable
fun <T> AnimateNullableContentVisibility(
    targetState: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(tween(easing = LinearEasing, durationMillis = 200)),
    exit: ExitTransition = fadeOut(tween(durationMillis = 200)),
    sizeTransform: SizeTransform? = null,
    label: String = "AnimateNullableContent",
    content: @Composable AnimatedContentScope.(targetState: T) -> Unit,
) {
    AnimatedContent(
        content = { state -> state?.let { content(it) } },
        targetState = targetState,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = enter,
                initialContentExit = exit,
                sizeTransform = sizeTransform,
            )
        },
        label = label,
        modifier = modifier,
    )
}

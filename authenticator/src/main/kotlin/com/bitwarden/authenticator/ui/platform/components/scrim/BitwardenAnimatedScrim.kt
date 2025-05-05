package com.bitwarden.authenticator.ui.platform.components.scrim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A scrim that animates its visibility.
 *
 * @param isVisible Whether or not the scrim should be visible. This controls the animation.
 * @param onClick A callback that is triggered when the scrim is clicked. No ripple will be
 * performed.
 * @param modifier A [Modifier] for the scrim's content.
 */
@Composable
fun BitwardenAnimatedScrim(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // Clear the ripple
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

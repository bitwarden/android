package com.x8bit.bitwarden.ui.platform.components.content

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A full screen loading effect which covers the entire screen and overlays either
 * a blur of the content or a translucent scrim. The content is not interactable and
 * the system back function is disabled while the loading content is displayed.
 *
 * @param showLoadingState whether or not to show the loading content overlay.
 * @param modifier the [Modifier] to be applied to the loading content.
 * @param message optional message to display above the loading spinner
 * @param content to be encased by the full screen loading content. If you want part of
 * your [Composable] covered by the effect it needs to be in this content block.
 */
@Composable
fun BitwardenFullScreenLoadingContent(
    showLoadingState: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = showLoadingState) {
        // No-op
    }
    ObscuredContent(
        enabled = showLoadingState,
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        content = content,
        overlayContent = {
            BitwardenLoadingContent(
                message = message,
                modifier = Modifier.align(Alignment.Center),
            )
        },
    )
}

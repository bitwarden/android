package com.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer

/**
 * Pins the calling composable in its [pinnableContainer] for as long as it stays in the
 * composition, so a lazy layout keeps it composed even after it scrolls out of view.
 *
 * @param pinnableContainer The [PinnableContainer] to pin, typically the current lazy item.
 */
@Composable
fun PinThisComposable(
    pinnableContainer: PinnableContainer? = LocalPinnableContainer.current,
) {
    DisposableEffect(pinnableContainer) {
        val pinnedHandle = pinnableContainer?.pin()
        onDispose { pinnedHandle?.release() }
    }
}

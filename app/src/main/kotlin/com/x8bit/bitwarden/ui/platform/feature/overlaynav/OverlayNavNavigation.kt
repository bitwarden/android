package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.navigation3.runtime.EntryProviderScope
import com.x8bit.bitwarden.ui.platform.feature.main.MainGraph

/**
 * Add the overlay navigation screen to the navigation entries.
 */
fun EntryProviderScope<MainGraph>.overlayNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    entry<MainGraph.OverlayNavRoute> {
        OverlayNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

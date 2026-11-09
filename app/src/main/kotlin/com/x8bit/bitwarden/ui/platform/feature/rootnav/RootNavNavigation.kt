@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.feature.overlaynav.OverlayNavGraph

/**
 * Add the root navigation screen to the navigation entries.
 */
fun EntryProviderScope<OverlayNavGraph>.rootNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    entry<OverlayNavGraph.RootNavigationRoute> {
        RootNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

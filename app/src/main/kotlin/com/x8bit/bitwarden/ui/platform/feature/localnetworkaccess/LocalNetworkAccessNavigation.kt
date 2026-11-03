@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.overlaynav.OverlayNavGraph

/**
 * Add the local network access screen to the navigation entries.
 */
fun EntryProviderScope<OverlayNavGraph>.localNetworkAccessDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    slideUpEntry<OverlayNavGraph.LocalNetworkAccessRoute> {
        LocalNetworkAccessScreen(onDismiss = onDismiss)
        // If we are displaying the local network access screen, then we can just hide
        // the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the local network access screen.
 */
fun NavigationController<OverlayNavGraph>.navigateToLocalNetworkAccess() {
    this.addSingleTop(route = OverlayNavGraph.LocalNetworkAccessRoute)
}

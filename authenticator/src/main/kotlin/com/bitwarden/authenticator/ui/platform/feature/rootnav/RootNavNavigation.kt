@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.main.MainGraph

/**
 * Add the root navigation screen to the navigation entries.
 */
fun EntryProviderScope<MainGraph>.rootNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    entry<MainGraph.RootNavigationRoute> {
        RootNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

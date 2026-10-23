package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.main.MainGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry

/**
 * Navigate to the debug menu screen.
 */
fun NavigationController<MainGraph>.navigateToDebugMenuScreen() {
    this.addSingleTop(route = MainGraph.DebugRoute)
}

/**
 * Add the debug menu screen to the navigation entries.
 */
fun EntryProviderScope<MainGraph>.debugMenuDestination(
    onNavigateBack: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    slideUpEntry<MainGraph.DebugRoute> {
        DebugMenuScreen(onNavigateBack = onNavigateBack)
        // If we are displaying the debug screen, then we can just hide the splash screen.
        onSplashScreenRemoved()
    }
}

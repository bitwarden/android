package com.x8bit.bitwarden.ui.platform.feature.splash

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.util.rootTransitionEntry
import com.bitwarden.ui.platform.theme.RootTransition
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph

/**
 * Add the splash screen to the navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.splashDestination() {
    rootTransitionEntry<RootNavGraph.SplashRoute>(rootTransition = RootTransition.NONE) {
        SplashScreen()
    }
}

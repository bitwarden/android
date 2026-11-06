package com.bitwarden.authenticator.ui.platform.feature.splash

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.ui.platform.navigation.util.rootTransitionEntry
import com.bitwarden.ui.platform.theme.RootTransition

/**
 * Add splash destinations to the navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.splashDestination() {
    rootTransitionEntry<RootNavGraph.SplashRoute>(rootTransition = RootTransition.NONE) {
        SplashScreen()
    }
}

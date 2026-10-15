package com.bitwarden.authenticator.ui.auth.unlock

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavGraph

/**
 * Add the unlock screen to the navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.unlockDestination() {
    entry<RootNavGraph.UnlockRoute> {
        UnlockScreen()
    }
}

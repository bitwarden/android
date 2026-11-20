package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.navigation3.runtime.EntryProviderScope
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph

/**
 * Add the Remove Password screen to the navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.removePasswordDestination() {
    entry<RootNavGraph.RemovePasswordRoute> {
        RemovePasswordScreen()
    }
}

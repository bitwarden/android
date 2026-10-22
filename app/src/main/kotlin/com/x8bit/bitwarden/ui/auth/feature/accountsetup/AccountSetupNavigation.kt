@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootTransitionEntry
import com.bitwarden.ui.platform.theme.RootTransition
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph

/**
 * Add the account setup section to the root navigation entries. The
 * [RootNavGraph.AccountSetupRoute] hosts the [AccountSetupGraph].
 */
fun EntryProviderScope<RootNavGraph>.accountSetupGraph() {
    rootTransitionEntry<RootNavGraph.AccountSetupRoute>(
        rootTransition = RootTransition.PUSH,
    ) { route ->
        val navigationController = rememberNavigationController(
            routes = route.backStack.toTypedArray(),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                setupUnlockDestinationAsRoot()
                setupAutoFillDestinationAsRoot()
                setupBrowserAutofillDestinationAsRoot()
                setupCompleteDestination()
            },
        )
    }
}

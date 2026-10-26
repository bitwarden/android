@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.autofill

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootPushEntry

/**
 * Add the Autofill test flow to the root navigation entries. The [RootNavGraph.AutofillRoute]
 * hosts the [AutofillGraph].
 */
fun EntryProviderScope<RootNavGraph>.autofillGraph(
    onNavigateBack: () -> Unit,
) {
    entry<RootNavGraph.AutofillRoute> {
        val navigationController = rememberNavigationController<AutofillGraph>(
            routes = arrayOf(AutofillGraph.AutofillPlaceholderRoute),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                autofillPlaceholderDestination(onNavigateBack = onNavigateBack)
            },
        )
    }
}

private fun EntryProviderScope<AutofillGraph>.autofillPlaceholderDestination(
    onNavigateBack: () -> Unit,
) {
    rootPushEntry<AutofillGraph.AutofillPlaceholderRoute> {
        AutofillPlaceholderScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Autofill test flow.
 */
fun NavigationController<RootNavGraph>.navigateToAutofill() {
    this.navigate(route = RootNavGraph.AutofillRoute)
}

@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.popUpTo
import com.bitwarden.ui.platform.navigation.util.rootPushEntry
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph

/**
 * Add the [SelectAccountScreen] to the navigation entries.
 */
fun EntryProviderScope<ExportItemsGraph>.selectAccountDestination(
    onAccountSelected: (id: String, hasOtherAccounts: Boolean) -> Unit,
) {
    rootPushEntry<ExportItemsGraph.SelectAccountRoute> {
        SelectAccountScreen(
            onAccountSelected = onAccountSelected,
        )
    }
}

/**
 * Navigate to the [SelectAccountScreen].
 */
fun NavigationController<ExportItemsGraph>.navigateToSelectAccountScreen() {
    this.navigate(route = ExportItemsGraph.SelectAccountRoute)
}

/**
 * Pop up to the [SelectAccountScreen].
 */
fun NavigationController<ExportItemsGraph>.popUpToSelectAccountScreen() {
    this.popUpTo<ExportItemsGraph.SelectAccountRoute>(inclusive = false)
}

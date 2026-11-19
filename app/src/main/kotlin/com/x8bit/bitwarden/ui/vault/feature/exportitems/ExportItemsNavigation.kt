@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.navigateToReviewExport
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.reviewExportDestination
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.selectAccountDestination
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.navigateToVerifyPassword
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.verifyPasswordDestination

/**
 * Add the export items section to the root navigation entries. The [RootNavGraph.ExportItemsRoute]
 * hosts the [ExportItemsGraph].
 */
fun EntryProviderScope<RootNavGraph>.exportItemsGraph() {
    entry<RootNavGraph.ExportItemsRoute> { route ->
        val navigationController = rememberNavigationController(
            routes = route.backStack.toTypedArray(),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                selectAccountDestination(
                    onAccountSelected = { userId, hasOtherAccounts ->
                        navigationController.navigateToVerifyPassword(
                            userId = userId,
                            hasOtherAccounts = hasOtherAccounts,
                        )
                    },
                )
                verifyPasswordDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                    onPasswordVerified = {
                        navigationController.navigateToReviewExport()
                    },
                )
                reviewExportDestination(navigationController = navigationController)
            },
        )
    }
}

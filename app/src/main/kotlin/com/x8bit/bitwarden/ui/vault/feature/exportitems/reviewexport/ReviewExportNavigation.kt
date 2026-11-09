@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.popUpToSelectAccountScreen

/**
 * Defines the destination for the Review Export screen within the navigation entries.
 *
 * This extension function on [EntryProviderScope] sets up the [ReviewExportScreen]
 * composable for the [ExportItemsGraph.ReviewExportRoute], using push transitions.
 *
 * @param navigationController The [NavigationController] used for handling back navigation from
 * the screen.
 */
fun EntryProviderScope<ExportItemsGraph>.reviewExportDestination(
    navigationController: NavigationController<ExportItemsGraph>,
) {
    pushEntry<ExportItemsGraph.ReviewExportRoute> {
        ReviewExportScreen(
            onNavigateBack = { navigationController.navigateBack() },
            onNavigateToAccountSelection = { navigationController.popUpToSelectAccountScreen() },
        )
    }
}

/**
 * Navigates to the Review Export screen.
 *
 * This extension function on [NavigationController] provides a type-safe way to navigate
 * to the [ExportItemsGraph.ReviewExportRoute].
 */
fun NavigationController<ExportItemsGraph>.navigateToReviewExport() {
    this.navigate(route = ExportItemsGraph.ReviewExportRoute)
}

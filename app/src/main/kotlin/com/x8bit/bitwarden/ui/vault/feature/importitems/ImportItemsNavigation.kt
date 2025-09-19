@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import items graph.
 */
@Serializable
data object ImportItemsGraphRoute

/**
 * Helper function to navigate to the import items screen.
 */
fun NavController.navigateToImportItemsGraph(
    navOptions: NavOptions? = null,
) {
    navigate(route = ImportItemsGraphRoute, navOptions = navOptions)
}

/**
 * Add the import items graph to the nav graph.
 */
fun NavGraphBuilder.importItemsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToMyVault: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
) {
    composableWithPushTransitions<ImportItemsGraphRoute> {
        ImportItemsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVault = onNavigateToMyVault,
            onNavigateToImportFromComputer = onNavigateToImportLogins,
        )
    }
}

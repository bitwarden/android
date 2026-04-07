@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import items screen.
 */
@Serializable
data object ImportItemsRoute

/**
 * Helper function to navigate to the import items screen.
 */
fun NavController.navigateToImportItemsScreen(
    navOptions: NavOptions? = null,
) {
    navigate(route = ImportItemsRoute, navOptions = navOptions)
}

/**
 * Add the import items screen to the nav graph.
 */
fun NavGraphBuilder.importItemsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
) {
    composableWithPushTransitions<ImportItemsRoute> {
        ImportItemsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToImportFromComputer = onNavigateToImportLogins,
        )
    }
}

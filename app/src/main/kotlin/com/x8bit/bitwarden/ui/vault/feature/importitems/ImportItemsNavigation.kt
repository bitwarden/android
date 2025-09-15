package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
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
 * Adds the import items screen to the navigation graph.
 */
fun NavGraphBuilder.importItemsScreenDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
) {
    composableWithSlideTransitions<ImportItemsRoute> {
        ImportItemsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVault = onNavigateToVault,
        )
    }
}

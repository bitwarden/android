package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import logins screen.
 */
@Serializable
data object ImportLoginsRoute

/**
 * Helper function to navigate to the import logins screen.
 */
fun NavController.navigateToImportLoginsScreen(
    navOptions: NavOptions? = null,
) {
    navigate(route = ImportLoginsRoute, navOptions = navOptions)
}

/**
 * Adds the import logins screen to the navigation graph.
 */
fun NavGraphBuilder.importLoginsScreenDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<ImportLoginsRoute> {
        ImportLoginsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

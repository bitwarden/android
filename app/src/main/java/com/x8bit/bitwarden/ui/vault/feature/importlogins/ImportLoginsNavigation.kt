package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val IMPORT_LOGINS_ROUTE = "import-logins"

/**
 * Helper function to navigate to the import logins screen.
 */
fun NavController.navigateToImportLoginsScreen(navOptions: NavOptions? = null) {
    navigate(route = IMPORT_LOGINS_ROUTE, navOptions = navOptions)
}

/**
 * Adds the import logins screen to the navigation graph.
 */
fun NavGraphBuilder.importLoginsScreenDestination(
    onNavigateBack: () -> Unit,
    onNavigateToImportSuccessScreen: () -> Unit,
) {
    composableWithSlideTransitions(
        route = IMPORT_LOGINS_ROUTE,
    ) {
        ImportLoginsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToImportSuccessScreen = onNavigateToImportSuccessScreen,
        )
    }
}

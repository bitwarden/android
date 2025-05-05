package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.authenticator.ui.platform.base.util.composableWithSlideTransitions

const val IMPORT_ROUTE = "importing"

/**
 * Add the import screen to the nav graph.
 */
fun NavGraphBuilder.importingDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(IMPORT_ROUTE) {
        ImportingScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Import destination.
 */
fun NavController.navigateToImporting(navOptions: NavOptions? = null) {
    navigate(IMPORT_ROUTE, navOptions)
}

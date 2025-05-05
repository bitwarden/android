package com.bitwarden.authenticator.ui.platform.feature.settings.export

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.authenticator.ui.platform.base.util.composableWithSlideTransitions

/**
 * Route for the export data screen.
 */
const val EXPORT_ROUTE = "export"

/**
 * Add the export data destination to the nav graph.
 */
fun NavGraphBuilder.exportDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(EXPORT_ROUTE) {
        ExportScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the export data screen.
 */
fun NavController.navigateToExport(navOptions: NavOptions? = null) {
    navigate(EXPORT_ROUTE, navOptions)
}

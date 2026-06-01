package com.bitwarden.authenticator.ui.platform.feature.settings.export

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the export screen.
 */
@Serializable
data object ExportRoute

/**
 * Add the export data destination to the nav graph.
 */
fun NavGraphBuilder.exportDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<ExportRoute> {
        ExportScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the export data screen.
 */
fun NavController.navigateToExport(navOptions: NavOptions? = null) {
    navigate(route = ExportRoute, navOptions = navOptions)
}

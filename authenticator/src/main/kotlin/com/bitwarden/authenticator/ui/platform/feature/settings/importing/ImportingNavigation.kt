package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import screen.
 */
@Serializable
data object ImportRoute

/**
 * Add the import screen to the nav graph.
 */
fun NavGraphBuilder.importingDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<ImportRoute> {
        ImportingScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Import destination.
 */
fun NavController.navigateToImporting(navOptions: NavOptions? = null) {
    navigate(route = ImportRoute, navOptions = navOptions)
}

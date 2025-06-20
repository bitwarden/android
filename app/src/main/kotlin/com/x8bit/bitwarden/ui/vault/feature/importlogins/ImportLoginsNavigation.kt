package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import logins screen.
 */
@Serializable
data object ImportLoginsRoute

/**
 * Arguments for the [ImportLoginsScreen] using [SavedStateHandle].
 */
data class ImportLoginsArgs(val snackBarRelay: SnackbarRelay)

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

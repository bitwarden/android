package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the import logins screen.
 */
@Serializable
data class ImportLoginsRoute(
    val snackbarRelay: SnackbarRelay,
)

/**
 * Arguments for the [ImportLoginsScreen] using [SavedStateHandle].
 */
data class ImportLoginsArgs(val snackBarRelay: SnackbarRelay)

/**
 * Constructs a [ImportLoginsArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toImportLoginsArgs(): ImportLoginsArgs {
    val route = this.toRoute<ImportLoginsRoute>()
    return ImportLoginsArgs(snackBarRelay = route.snackbarRelay)
}

/**
 * Helper function to navigate to the import logins screen.
 */
fun NavController.navigateToImportLoginsScreen(
    snackbarRelay: SnackbarRelay,
    navOptions: NavOptions? = null,
) {
    navigate(route = ImportLoginsRoute(snackbarRelay = snackbarRelay), navOptions = navOptions)
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

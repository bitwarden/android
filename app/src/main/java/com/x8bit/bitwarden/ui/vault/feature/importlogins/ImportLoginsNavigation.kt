package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay

private const val IMPORT_LOGINS_PREFIX = "import-logins"
private const val IMPORT_LOGINS_NAV_ARG = "snackbarRelay"
private const val IMPORT_LOGINS_ROUTE = "$IMPORT_LOGINS_PREFIX/{$IMPORT_LOGINS_NAV_ARG}"

/**
 * Arguments for the [ImportLoginsScreen] using [SavedStateHandle].
 */
@OmitFromCoverage
data class ImportLoginsArgs(val snackBarRelay: SnackbarRelay) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        snackBarRelay = SnackbarRelay.valueOf(
            requireNotNull(savedStateHandle[IMPORT_LOGINS_NAV_ARG]),
        ),
    )
}

/**
 * Helper function to navigate to the import logins screen.
 */
fun NavController.navigateToImportLoginsScreen(
    snackbarRelay: SnackbarRelay,
    navOptions: NavOptions? = null,
) {
    navigate(route = "$IMPORT_LOGINS_PREFIX/$snackbarRelay", navOptions = navOptions)
}

/**
 * Adds the import logins screen to the navigation graph.
 */
fun NavGraphBuilder.importLoginsScreenDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = IMPORT_LOGINS_ROUTE,
        arguments = listOf(
            navArgument(IMPORT_LOGINS_NAV_ARG) {
                type = NavType.StringType
                nullable = false
            },
        ),
    ) {
        ImportLoginsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

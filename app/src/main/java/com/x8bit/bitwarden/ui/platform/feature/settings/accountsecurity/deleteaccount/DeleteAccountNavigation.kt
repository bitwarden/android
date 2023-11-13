package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val DELETE_ACCOUNT_ROUTE = "delete_account"

/**
 * Add delete account destinations to the nav graph.
 */
fun NavGraphBuilder.deleteAccountDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = DELETE_ACCOUNT_ROUTE,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        DeleteAccountScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the delete account screen.
 */
fun NavController.navigateToDeleteAccount(navOptions: NavOptions? = null) {
    navigate(DELETE_ACCOUNT_ROUTE, navOptions)
}

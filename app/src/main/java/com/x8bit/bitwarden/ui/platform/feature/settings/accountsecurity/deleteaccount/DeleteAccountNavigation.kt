package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val DELETE_ACCOUNT_ROUTE = "delete_account"

/**
 * Add delete account destinations to the nav graph.
 */
fun NavGraphBuilder.deleteAccountDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccountConfirmation: () -> Unit,
) {
    composableWithSlideTransitions(
        route = DELETE_ACCOUNT_ROUTE,
    ) {
        DeleteAccountScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDeleteAccountConfirmation = onNavigateToDeleteAccountConfirmation,
        )
    }
}

/**
 * Navigate to the delete account screen.
 */
fun NavController.navigateToDeleteAccount(navOptions: NavOptions? = null) {
    navigate(DELETE_ACCOUNT_ROUTE, navOptions)
}

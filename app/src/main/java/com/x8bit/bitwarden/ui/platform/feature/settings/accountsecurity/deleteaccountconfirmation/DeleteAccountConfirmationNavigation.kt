package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val DELETE_ACCOUNT_CONFIRMATION_ROUTE = "delete_account_confirmation"

/**
 * Add delete account confirmation destinations to the nav graph.
 */
fun NavGraphBuilder.deleteAccountConfirmationDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = DELETE_ACCOUNT_CONFIRMATION_ROUTE,
    ) {
        DeleteAccountConfirmationScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the [DeleteAccountConfirmationScreen].
 */
fun NavController.navigateToDeleteAccountConfirmation(navOptions: NavOptions? = null) {
    navigate(DELETE_ACCOUNT_CONFIRMATION_ROUTE, navOptions)
}

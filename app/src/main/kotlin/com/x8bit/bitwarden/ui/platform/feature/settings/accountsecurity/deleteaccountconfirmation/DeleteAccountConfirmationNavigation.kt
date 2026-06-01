package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the delete account confirmation screen.
 */
@Serializable
data object DeleteAccountConfirmationRoute

/**
 * Add delete account confirmation destinations to the nav graph.
 */
fun NavGraphBuilder.deleteAccountConfirmationDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<DeleteAccountConfirmationRoute> {
        DeleteAccountConfirmationScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the [DeleteAccountConfirmationScreen].
 */
fun NavController.navigateToDeleteAccountConfirmation(navOptions: NavOptions? = null) {
    this.navigate(route = DeleteAccountConfirmationRoute, navOptions = navOptions)
}

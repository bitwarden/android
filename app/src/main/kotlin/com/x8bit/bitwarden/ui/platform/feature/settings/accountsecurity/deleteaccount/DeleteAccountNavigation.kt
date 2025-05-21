package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the delete account screen.
 */
@Serializable
data object DeleteAccountRoute

/**
 * Add delete account destinations to the nav graph.
 */
fun NavGraphBuilder.deleteAccountDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccountConfirmation: () -> Unit,
) {
    composableWithSlideTransitions<DeleteAccountRoute> {
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
    this.navigate(route = DeleteAccountRoute, navOptions = navOptions)
}

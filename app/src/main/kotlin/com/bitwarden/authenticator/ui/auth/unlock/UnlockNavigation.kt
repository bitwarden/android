package com.bitwarden.authenticator.ui.auth.unlock

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val UNLOCK_ROUTE: String = "unlock"

/**
 * Navigate to the unlock screen.
 */
fun NavController.navigateToUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(route = UNLOCK_ROUTE, navOptions = navOptions)
}

/**
 * Add the unlock screen to the nav graph.
 */
fun NavGraphBuilder.unlockDestination(
    onUnlocked: () -> Unit,
) {
    composable(route = UNLOCK_ROUTE) {
        UnlockScreen(onUnlocked = onUnlocked)
    }
}

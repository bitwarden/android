package com.bitwarden.authenticator.ui.auth.unlock

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the unlock screen.
 */
@Serializable
data object UnlockRoute

/**
 * Navigate to the unlock screen.
 */
fun NavController.navigateToUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(route = UnlockRoute, navOptions = navOptions)
}

/**
 * Add the unlock screen to the nav graph.
 */
fun NavGraphBuilder.unlockDestination(
    onUnlocked: () -> Unit,
) {
    composable<UnlockRoute> {
        UnlockScreen(onUnlocked = onUnlocked)
    }
}

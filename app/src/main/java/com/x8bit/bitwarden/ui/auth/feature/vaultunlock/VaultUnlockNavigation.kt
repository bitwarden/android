package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val VAULT_UNLOCK: String = "vault_unlock"

/**
 * Navigate to the Vault Unlock screen.
 */
fun NavController.navigateToVaultUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(VAULT_UNLOCK, navOptions)
}

/**
 * Add the Vault Unlock screen to the nav graph.
 */
fun NavGraphBuilder.vaultUnlockDestinations() {
    composable(
        route = VAULT_UNLOCK,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        VaultUnlockScreen()
    }
}

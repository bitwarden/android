package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

const val VAULT_UNLOCK_ROUTE: String = "vault_unlock"

/**
 * Navigate to the Vault Unlock screen.
 */
fun NavController.navigateToVaultUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(VAULT_UNLOCK_ROUTE, navOptions)
}

/**
 * Add the Vault Unlock screen to the nav graph.
 */
fun NavGraphBuilder.vaultUnlockDestination() {
    composableWithSlideTransitions(
        route = VAULT_UNLOCK_ROUTE,
    ) {
        VaultUnlockScreen()
    }
}

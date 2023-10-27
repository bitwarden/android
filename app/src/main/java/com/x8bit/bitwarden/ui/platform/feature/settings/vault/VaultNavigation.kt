package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

private const val VAULT_ROUTE = "settings_vault"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.vaultDestination(
    onNavigateBack: () -> Unit,
) {
    composable(VAULT_ROUTE) {
        VaultScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the vault screen.
 */
fun NavController.navigateToVault(navOptions: NavOptions? = null) {
    navigate(VAULT_ROUTE, navOptions)
}

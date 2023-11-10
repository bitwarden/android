package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val VAULT_ROUTE: String = "vault"

/**
 * Add vault destination to the nav graph.
 */
fun NavGraphBuilder.vaultDestination(
    onNavigateToVaultAddItemScreen: () -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
) {
    composable(VAULT_ROUTE) {
        VaultScreen(
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
        )
    }
}

/**
 * Navigate to the [VaultScreen].
 */
fun NavController.navigateToVault(navOptions: NavOptions? = null) {
    navigate(VAULT_ROUTE, navOptions)
}

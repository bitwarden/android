package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

const val VAULT_ROUTE: String = "vault"

/**
 * Add vault destination to the nav graph.
 */
fun NavGraphBuilder.vaultDestination(
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
) {
    composable(
        route = VAULT_ROUTE,
        enterTransition = TransitionProviders.Enter.stay,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushRight,
        popExitTransition = TransitionProviders.Exit.fadeOut,
    ) {
        VaultScreen(
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = onNavigateToVaultItemListingScreen,
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

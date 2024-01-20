package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestination
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.navigateToVerificationCodeScreen
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.vaultVerificationCodeDestination

const val VAULT_GRAPH_ROUTE: String = "vault_graph"

/**
 * Add vault destinations to the nav graph.
 */
fun NavGraphBuilder.vaultGraph(
    navController: NavController,
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
) {
    navigation(
        route = VAULT_GRAPH_ROUTE,
        startDestination = VAULT_ROUTE,
    ) {
        vaultDestination(
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = { navController.navigateToVaultItemListing(it) },
            onNavigateToVerificationCodeScreen = {
                navController.navigateToVerificationCodeScreen()
            },

            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
        )
        vaultItemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
        )

        vaultVerificationCodeDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
        )
    }
}

/**
 * Navigate to the vault graph.
 */
fun NavController.navigateToVaultGraph(navOptions: NavOptions? = null) {
    navigate(VAULT_GRAPH_ROUTE, navOptions)
}

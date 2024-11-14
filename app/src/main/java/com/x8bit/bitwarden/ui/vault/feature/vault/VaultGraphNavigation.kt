package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestination
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.navigateToVerificationCodeScreen
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.vaultVerificationCodeDestination
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

const val VAULT_GRAPH_ROUTE: String = "vault_graph"

/**
 * Add vault destinations to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultGraph(
    navController: NavController,
    onNavigateToVaultAddItemScreen: (
        vaultItemCipherType: VaultItemCipherType,
        selectedFolderId: String?,
        selectedCollectionId: String?,
    ) -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    navigation(
        route = VAULT_GRAPH_ROUTE,
        startDestination = VAULT_ROUTE,
    ) {
        vaultDestination(
            onNavigateToVaultAddItemScreen = {
                onNavigateToVaultAddItemScreen(VaultItemCipherType.LOGIN, null, null)
            },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = { navController.navigateToVaultItemListing(it) },
            onNavigateToVerificationCodeScreen = {
                navController.navigateToVerificationCodeScreen()
            },
            onNavigateToSearchVault = onNavigateToSearchVault,
            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
            onNavigateToImportLogins = onNavigateToImportLogins,
        )
        vaultItemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListing = {
                navController.navigateToVaultItemListing(it)
            },
        )

        vaultVerificationCodeDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSearchVault = {
                onNavigateToSearchVault(SearchType.Vault.VerificationCodes)
            },
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

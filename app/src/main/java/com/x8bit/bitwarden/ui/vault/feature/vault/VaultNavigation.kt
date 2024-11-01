package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

const val VAULT_ROUTE: String = "vault"

/**
 * Add vault destination to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultDestination(
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToVerificationCodeScreen: () -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    composableWithRootPushTransitions(
        route = VAULT_ROUTE,
    ) {
        VaultScreen(
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = onNavigateToVaultItemListingScreen,
            onNavigateToVerificationCodeScreen = onNavigateToVerificationCodeScreen,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
            onNavigateToImportLogins = onNavigateToImportLogins,
        )
    }
}

/**
 * Navigate to the [VaultScreen].
 */
fun NavController.navigateToVault(navOptions: NavOptions? = null) {
    navigate(VAULT_ROUTE, navOptions)
}

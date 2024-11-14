package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithStayTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * The functions below pertain to entry into the [VaultUnlockedNavBarScreen].
 */
const val VAULT_UNLOCKED_NAV_BAR_ROUTE: String = "VaultUnlockedNavBar"

/**
 * Navigate to the [VaultUnlockedNavBarScreen].
 */
fun NavController.navigateToVaultUnlockedNavBar(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_NAV_BAR_ROUTE, navOptions)
}

/**
 * Add vault unlocked destination to the root nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultUnlockedNavBarDestination(
    onNavigateToVaultAddItem: (VaultItemCipherType, String?, String?) -> Unit,
    onNavigateToVaultItem: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    composableWithStayTransitions(
        route = VAULT_UNLOCKED_NAV_BAR_ROUTE,
    ) {
        VaultUnlockedNavBarScreen(
            onNavigateToVaultAddItem = onNavigateToVaultAddItem,
            onNavigateToVaultItem = onNavigateToVaultItem,
            onNavigateToVaultEditItem = onNavigateToVaultEditItem,
            onNavigateToSearchSend = onNavigateToSearchSend,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onNavigateToAddSend = onNavigateToAddSend,
            onNavigateToEditSend = onNavigateToEditSend,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToPendingRequests = onNavigateToPendingRequests,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
            onNavigateToSetupAutoFillScreen = onNavigateToSetupAutoFillScreen,
            onNavigateToImportLogins = onNavigateToImportLogins,
        )
    }
}

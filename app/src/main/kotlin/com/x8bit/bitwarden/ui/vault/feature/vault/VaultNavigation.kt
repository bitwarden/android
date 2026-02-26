package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault screen.
 */
@Serializable
data object VaultRoute

/**
 * Add vault destination to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultDestination(
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVerificationCodeScreen: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToAutofillScreen: () -> Unit,
) {
    composableWithRootPushTransitions<VaultRoute> {
        VaultScreen(
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = onNavigateToVaultItemListingScreen,
            onNavigateToVerificationCodeScreen = onNavigateToVerificationCodeScreen,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
            onNavigateToImportLogins = onNavigateToImportLogins,
            onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
            onNavigateToAboutScreen = onNavigateToAboutScreen,
            onNavigateToAutofillScreen = onNavigateToAutofillScreen,
        )
    }
}

/**
 * Navigate to the [VaultScreen].
 */
fun NavController.navigateToVault(navOptions: NavOptions? = null) {
    this.navigate(route = VaultRoute, navOptions = navOptions)
}

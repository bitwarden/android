package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestination
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.navigateToVerificationCodeScreen
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.vaultVerificationCodeDestination

/**
 * Add the vault destinations to the vault navigation entries. The [VaultGraph.VaultRoute] is the
 * root of the vault tab's [navigationController].
 */
@Suppress("LongParameterList")
fun EntryProviderScope<VaultGraph>.vaultDestinations(
    navigationController: NavigationController<VaultGraph>,
    onNavigateToVaultAddItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToVaultItemScreen: (route: VaultUnlockedGraph.VaultItemRoute) -> Unit,
    onNavigateToVaultEditItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToAutofillScreen: () -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    vaultDestination(
        onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
        onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
        onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
        onNavigateToVaultItemListingScreen = {
            navigationController.navigateToVaultItemListing(vaultItemListingType = it)
        },
        onNavigateToVerificationCodeScreen = {
            navigationController.navigateToVerificationCodeScreen()
        },
        onNavigateToSearchVault = onNavigateToSearchVault,
        onDimBottomNavBarRequest = onDimBottomNavBarRequest,
        onNavigateToImportLogins = onNavigateToImportLogins,
        onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        onNavigateToAutofillScreen = onNavigateToAutofillScreen,
        onNavigateToPlan = onNavigateToPlan,
    )
    vaultItemListingDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
        onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
        onNavigateToSearchVault = onNavigateToSearchVault,
        onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
        onNavigateToVaultItemListing = {
            navigationController.navigateToVaultItemListing(vaultItemListingType = it)
        },
        onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
        onNavigateToPlan = onNavigateToPlan,
    )
    vaultVerificationCodeDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToSearchVault = { onNavigateToSearchVault(SearchType.Vault.VerificationCodes) },
        onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
    )
}

package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToVaultItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestination
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.navigateToVerificationCodeScreen
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.vaultVerificationCodeDestination
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault graph.
 */
@Serializable
data object VaultGraphRoute

/**
 * Add vault destinations to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultGraph(
    navController: NavController,
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToImportItems: () -> Unit,
    onNavigateToMyVault: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToAutofillScreen: () -> Unit,
) {
    navigation<VaultGraphRoute>(
        startDestination = VaultRoute,
    ) {
        vaultDestination(
            onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreen(it) },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListingScreen = { navController.navigateToVaultItemListing(it) },
            onNavigateToVerificationCodeScreen = {
                navController.navigateToVerificationCodeScreen()
            },
            onNavigateToSearchVault = onNavigateToSearchVault,
            onDimBottomNavBarRequest = onDimBottomNavBarRequest,
            onNavigateToImportLogins = onNavigateToImportLogins,
            onNavigateToImportItems = onNavigateToImportItems,
            onNavigateToMyVault = onNavigateToMyVault,
            onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
            onNavigateToAboutScreen = onNavigateToAboutScreen,
            onNavigateToAutofillScreen = onNavigateToAutofillScreen,
        )
        vaultItemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultItemListing = { navController.navigateToVaultItemListing(it) },
            onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
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
    this.navigate(route = VaultGraphRoute, navOptions = navOptions)
}

/**
 * Navigate to the vault graph root.
 */
fun NavController.navigateToVaultGraphRoot() {
    // Brings up back to the Vault graph
    navigateToVaultGraph(
        navOptions = navOptions {
            popUpTo(id = graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        },
    )
    // Then ensures that we are at the root
    popBackStack(route = VaultRoute, inclusive = false)
}

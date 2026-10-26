@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.bitwarden.ui.platform.navigation.util.stayEntry
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

/**
 * Add the [VaultItemListingScreen] to the navigation entries.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<VaultGraph>.vaultItemListingDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (route: VaultUnlockedGraph.VaultItemRoute) -> Unit,
    onNavigateToVaultEditItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    onNavigateToVaultAddItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    pushEntry<VaultGraph.VaultItemListingRoute> { route ->
        VaultItemListingScreen(
            viewModel = vaultItemListingViewModel(
                vaultItemListingType = route.vaultItemListingType,
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToAddEditSendItem = { },
            onNavigateToViewSendItem = { },
            onNavigateToVaultItemListing = onNavigateToVaultItemListing,
            onNavigateToSearch = { onNavigateToSearchVault(it as SearchType.Vault) },
            onNavigateToAddFolder = onNavigateToAddFolderScreen,
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

/**
 * Add the [VaultItemListingScreen] as the root of a back stack to the navigation entries.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<VaultUnlockedGraph>.vaultItemListingDestinationAsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (route: VaultUnlockedGraph.VaultItemRoute) -> Unit,
    onNavigateToVaultEditItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToVaultAddItemScreen: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    stayEntry<VaultUnlockedGraph.VaultItemListingRoute> { route ->
        VaultItemListingScreen(
            viewModel = vaultItemListingViewModel(
                vaultItemListingType = route.vaultItemListingType,
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToSearch = { onNavigateToSearchVault(it as SearchType.Vault) },
            onNavigateToAddFolder = onNavigateToAddFolderScreen,
            onNavigateToVaultItemListing = {},
            onNavigateToAddEditSendItem = {},
            onNavigateToViewSendItem = {},
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

/**
 * Add the [VaultItemListingScreen] for sends to the navigation entries.
 */
fun EntryProviderScope<SendGraph>.sendItemListingDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSendItem: (route: VaultUnlockedGraph.AddEditSendRoute) -> Unit,
    onNavigateToViewSendItem: (route: VaultUnlockedGraph.ViewSendRoute) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    pushEntry<SendGraph.SendItemListingRoute> { route ->
        VaultItemListingScreen(
            viewModel = vaultItemListingViewModel(
                vaultItemListingType = route.vaultItemListingType,
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToAddEditSendItem = onNavigateToAddEditSendItem,
            onNavigateToViewSendItem = onNavigateToViewSendItem,
            onNavigateToVaultAddItemScreen = { },
            onNavigateToAddFolder = { _ -> },
            onNavigateToVaultItemScreen = { },
            onNavigateToVaultEditItemScreen = { },
            onNavigateToVaultItemListing = { },
            onNavigateToSearch = { onNavigateToSearchSend(it as SearchType.Sends) },
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

@Composable
private fun vaultItemListingViewModel(
    vaultItemListingType: VaultItemListingType,
): VaultItemListingViewModel =
    hiltViewModel<VaultItemListingViewModel, VaultItemListingViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(vaultItemListingType = vaultItemListingType)
        },
    )

/**
 * Navigate to the [VaultItemListingScreen] for vault.
 */
fun NavigationController<VaultGraph>.navigateToVaultItemListing(
    vaultItemListingType: VaultItemListingType,
) {
    this.navigate(
        route = VaultGraph.VaultItemListingRoute(vaultItemListingType = vaultItemListingType),
    )
}

/**
 * Navigate to the [VaultItemListingScreen] for sends.
 */
fun NavigationController<SendGraph>.navigateToSendItemListing(
    vaultItemListingType: VaultItemListingType,
) {
    this.navigate(
        route = SendGraph.SendItemListingRoute(vaultItemListingType = vaultItemListingType),
    )
}

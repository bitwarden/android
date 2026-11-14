package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Add the vault add & edit screen to the navigation entries.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<VaultUnlockedGraph>.vaultAddEditDestination(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToQrCodeScanScreen: () -> Unit,
    onNavigateToCardScanScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToPlan: () -> Unit,
    onCloseAndNavigateToVaultItem: (cipherId: String, cipherType: VaultItemCipherType) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.VaultAddEditRoute> { route ->
        VaultAddEditScreen(
            viewModel = hiltViewModel<VaultAddEditViewModel, VaultAddEditViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToManualCodeEntryScreen = onNavigateToManualCodeEntryScreen,
            onNavigateToQrCodeScanScreen = onNavigateToQrCodeScanScreen,
            onNavigateToCardScanScreen = onNavigateToCardScanScreen,
            onNavigateToGeneratorModal = onNavigateToGeneratorModal,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
            onNavigateToPlan = onNavigateToPlan,
            onCloseAndNavigateToVaultItem = onCloseAndNavigateToVaultItem,
        )
    }
}

/**
 * Navigate to the vault add & edit screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToVaultAddEdit(
    route: VaultUnlockedGraph.VaultAddEditRoute,
) {
    this.navigate(route = route)
}

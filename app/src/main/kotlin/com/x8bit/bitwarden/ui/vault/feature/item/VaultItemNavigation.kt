package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the vault item screen to the navigation entries.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<VaultUnlockedGraph>.vaultItemDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultEditItem: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
    onNavigateToPasswordHistory: (vaultItemId: String) -> Unit,
    onNavigateToPreviewAttachment: (route: VaultUnlockedGraph.PreviewAttachmentRoute) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.VaultItemRoute> { route ->
        VaultItemScreen(
            viewModel = hiltViewModel<VaultItemViewModel, VaultItemViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToVaultAddEditItem = onNavigateToVaultEditItem,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateToPreviewAttachment = onNavigateToPreviewAttachment,
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

/**
 * Navigate to the vault item screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToVaultItem(
    route: VaultUnlockedGraph.VaultItemRoute,
) {
    this.navigate(route = route)
}

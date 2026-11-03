package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the vault move to organization screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.vaultMoveToOrganizationDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.VaultMoveToOrganizationRoute> { route ->
        VaultMoveToOrganizationScreen(
            viewModel = hiltViewModel(
                creationCallback = { factory: VaultMoveToOrganizationViewModel.Factory ->
                    factory.create(route = route)
                },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the vault move to organization screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToVaultMoveToOrganization(
    vaultItemId: String,
    showOnlyCollections: Boolean,
) {
    this.navigate(
        route = VaultUnlockedGraph.VaultMoveToOrganizationRoute(
            vaultItemId = vaultItemId,
            showOnlyCollections = showOnlyCollections,
        ),
    )
}

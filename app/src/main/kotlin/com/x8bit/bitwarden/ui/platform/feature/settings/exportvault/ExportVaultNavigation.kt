package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the Export Vault screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.exportVaultDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.ExportVaultRoute> {
        ExportVaultScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Export Vault screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToExportVault() {
    this.navigate(route = VaultUnlockedGraph.ExportVaultRoute)
}

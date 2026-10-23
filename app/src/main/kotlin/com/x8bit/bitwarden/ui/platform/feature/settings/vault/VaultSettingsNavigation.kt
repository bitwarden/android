package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the Vault Settings screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.vaultSettingsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToImportItems: () -> Unit,
) {
    pushEntry<SettingsGraph.VaultSettingsRoute> {
        VaultSettingsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToImportLogins = onNavigateToImportLogins,
            onNavigateToImportItems = onNavigateToImportItems,
        )
    }
}

/**
 * Navigate to the Vault Settings screen.
 */
fun NavigationController<SettingsGraph>.navigateToVaultSettings() {
    this.navigate(route = SettingsGraph.VaultSettingsRoute)
}

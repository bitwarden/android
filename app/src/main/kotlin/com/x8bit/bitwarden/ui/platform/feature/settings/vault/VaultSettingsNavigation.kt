package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault settings screen.
 */
@Serializable
data object VaultSettingsRoute

/**
 * Add Vault Settings destinations to the nav graph.
 */
fun NavGraphBuilder.vaultSettingsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToImportItems: () -> Unit,
) {
    composableWithPushTransitions<VaultSettingsRoute> {
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
fun NavController.navigateToVaultSettings(navOptions: NavOptions? = null) {
    this.navigate(route = VaultSettingsRoute, navOptions = navOptions)
}

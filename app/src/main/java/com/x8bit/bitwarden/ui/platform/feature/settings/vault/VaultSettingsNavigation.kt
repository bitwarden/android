package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay

private const val VAULT_SETTINGS_ROUTE = "vault_settings"

/**
 * Add Vault Settings destinations to the nav graph.
 */
fun NavGraphBuilder.vaultSettingsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    composableWithPushTransitions(
        route = VAULT_SETTINGS_ROUTE,
    ) {
        VaultSettingsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToImportLogins = onNavigateToImportLogins,
        )
    }
}

/**
 * Navigate to the Vault Settings screen.
 */
fun NavController.navigateToVaultSettings(navOptions: NavOptions? = null) {
    navigate(VAULT_SETTINGS_ROUTE, navOptions)
}

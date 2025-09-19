package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.vault.feature.importitems.importItemsGraph
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault settings screen.
 */
@Serializable
data object VaultSettingsRoute

/**
 * Add Vault Settings destinations to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultSettingsDestination(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToImportItems: () -> Unit,
    onNavigateToMyVault: () -> Unit,
) {
    composableWithPushTransitions<VaultSettingsRoute> {
        VaultSettingsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToImportLogins = onNavigateToImportLogins,
            onNavigateToImportItems = onNavigateToImportItems,
        )

        importItemsGraph(
            navController = navController,
            onNavigateBack = onNavigateBack,
            onNavigateToMyVault = onNavigateToMyVault,
        )
    }
}

/**
 * Navigate to the Vault Settings screen.
 */
fun NavController.navigateToVaultSettings(navOptions: NavOptions? = null) {
    this.navigate(route = VaultSettingsRoute, navOptions = navOptions)
}

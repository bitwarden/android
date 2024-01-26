package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EXPORT_VAULT_ROUTE = "export_vault"

/**
 * Add the Export Vault screen to the nav graph.
 */
fun NavGraphBuilder.exportVaultDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = EXPORT_VAULT_ROUTE,
    ) {
        ExportVaultScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Export Vault screen.
 */
fun NavController.navigateToExportVault(navOptions: NavOptions? = null) {
    this.navigate(EXPORT_VAULT_ROUTE, navOptions)
}

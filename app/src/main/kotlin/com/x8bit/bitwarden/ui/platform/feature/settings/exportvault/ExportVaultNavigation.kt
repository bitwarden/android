package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the pending requests screen.
 */
@Serializable
data object ExportVaultRoute

/**
 * Add the Export Vault screen to the nav graph.
 */
fun NavGraphBuilder.exportVaultDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<ExportVaultRoute> {
        ExportVaultScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Export Vault screen.
 */
fun NavController.navigateToExportVault(navOptions: NavOptions? = null) {
    this.navigate(route = ExportVaultRoute, navOptions = navOptions)
}

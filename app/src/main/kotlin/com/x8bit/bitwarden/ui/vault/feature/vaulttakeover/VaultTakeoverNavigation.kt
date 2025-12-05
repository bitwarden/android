package com.x8bit.bitwarden.ui.vault.feature.vaulttakeover

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault takeover screen.
 */
@Serializable
data object VaultTakeoverRoute

/**
 * Navigate to the vault takeover screen.
 */
fun NavController.navigateToVaultTakeover(navOptions: NavOptions? = null) {
    this.navigate(route = VaultTakeoverRoute, navOptions = navOptions)
}

/**
 * Add the vault takeover screen to the nav graph.
 */
fun NavGraphBuilder.vaultTakeoverDestination(
    onNavigateToVault: () -> Unit,
    onNavigateToLeaveOrganization: () -> Unit,
) {
    composableWithSlideTransitions<VaultTakeoverRoute> {
        VaultTakeoverScreen(
            onNavigateToVault = onNavigateToVault,
            onNavigateToLeaveOrganization = onNavigateToLeaveOrganization,
        )
    }
}

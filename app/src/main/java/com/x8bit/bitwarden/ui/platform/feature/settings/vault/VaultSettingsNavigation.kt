package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val VAULT_SETTINGS_ROUTE = "vault_settings"

/**
 * Add vault settings destinations to the nav graph.
 */
fun NavGraphBuilder.vaultSettingsDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = VAULT_SETTINGS_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        VaultSettingsScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the vault settings screen.
 */
fun NavController.navigateToVaultSettings(navOptions: NavOptions? = null) {
    navigate(VAULT_SETTINGS_ROUTE, navOptions)
}

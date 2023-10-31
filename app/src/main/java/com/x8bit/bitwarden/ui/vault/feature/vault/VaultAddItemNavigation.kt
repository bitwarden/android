package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val ADD_ITEM_ROUTE = "vault_add_item"

/**
 * Add the vault add item screen to the nav graph.
 */
fun NavGraphBuilder.vaultAddItemDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        ADD_ITEM_ROUTE,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        VaultAddItemScreen(onNavigateBack)
    }
}

/**
 * Navigate to the vault add item screen.
 */
fun NavController.navigateToVaultAddItem(navOptions: NavOptions? = null) {
    navigate(ADD_ITEM_ROUTE, navOptions)
}

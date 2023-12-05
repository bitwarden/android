package com.x8bit.bitwarden.ui.vault.feature.edit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val VAULT_EDIT_ITEM_PREFIX = "vault_edit_item"
private const val VAULT_EDIT_ITEM_ID = "vault_edit_item_id"
private const val VAULT_EDIT_ROUTE = "$VAULT_EDIT_ITEM_PREFIX/{$VAULT_EDIT_ITEM_ID}"

/**
 * Class to retrieve vault item arguments from the [SavedStateHandle].
 */
class VaultEditItemArgs(val vaultItemId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[VAULT_EDIT_ITEM_ID]) as String,
    )
}

/**
 * Add the vault edit item screen to the nav graph.
 */
fun NavGraphBuilder.vaultEditItemDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = VAULT_EDIT_ROUTE,
        arguments = listOf(
            navArgument(VAULT_EDIT_ITEM_ID) { type = NavType.StringType },
        ),
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        VaultEditItemScreen(onNavigateBack)
    }
}

/**
 * Navigate to the vault edit item screen.
 */
fun NavController.navigateToVaultEditItem(
    vaultItemId: String,
    navOptions: NavOptions? = null,
) {
    navigate("$VAULT_EDIT_ITEM_PREFIX/$vaultItemId", navOptions)
}

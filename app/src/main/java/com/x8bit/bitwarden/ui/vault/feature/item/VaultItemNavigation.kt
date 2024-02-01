package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val VAULT_ITEM_PREFIX = "vault_item"
private const val VAULT_ITEM_ID = "vault_item_id"
private const val VAULT_ITEM_ROUTE = "$VAULT_ITEM_PREFIX/{$VAULT_ITEM_ID}"

/**
 * Class to retrieve vault item arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultItemArgs(val vaultItemId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[VAULT_ITEM_ID]) as String,
    )
}

/**
 * Add the vault item screen to the nav graph.
 */
fun NavGraphBuilder.vaultItemDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String, isClone: Boolean) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
    onNavigateToPasswordHistory: (vaultItemId: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = VAULT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(VAULT_ITEM_ID) { type = NavType.StringType },
        ),
    ) {
        VaultItemScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultAddEditItem = onNavigateToVaultEditItem,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
        )
    }
}

/**
 * Navigate to the vault item screen.
 */
fun NavController.navigateToVaultItem(
    vaultItemId: String,
    navOptions: NavOptions? = null,
) {
    navigate("$VAULT_ITEM_PREFIX/$vaultItemId", navOptions)
}

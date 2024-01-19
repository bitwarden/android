package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val VAULT_MOVE_TO_ORGANIZATION_PREFIX = "vault_move_to_organization"
private const val VAULT_MOVE_TO_ORGANIZATION_ID = "vault_move_to_organization_id"
private const val VAULT_MOVE_TO_ORGANIZATION_ROUTE =
    "$VAULT_MOVE_TO_ORGANIZATION_PREFIX/{$VAULT_MOVE_TO_ORGANIZATION_ID}"

/**
 * Class to retrieve vault move to organization arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultMoveToOrganizationArgs(val vaultItemId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[VAULT_MOVE_TO_ORGANIZATION_ID]) as String,
    )
}

/**
 * Add the vault move to organization screen to the nav graph.
 */
fun NavGraphBuilder.vaultMoveToOrganizationDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = VAULT_MOVE_TO_ORGANIZATION_ROUTE,
        arguments = listOf(
            navArgument(VAULT_MOVE_TO_ORGANIZATION_ID) { type = NavType.StringType },
        ),
    ) {
        VaultMoveToOrganizationScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the vault move to organization screen.
 */
fun NavController.navigateToVaultMoveToOrganization(
    vaultItemId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$VAULT_MOVE_TO_ORGANIZATION_PREFIX/$vaultItemId",
        navOptions = navOptions,
    )
}

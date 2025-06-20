package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault move to organization screen.
 */
@Serializable
data class VaultMoveToOrganizationRoute(
    val vaultItemId: String,
    val showOnlyCollections: Boolean,
)

/**
 * Class to retrieve vault move to organization arguments from the [SavedStateHandle].
 */
data class VaultMoveToOrganizationArgs(
    val vaultItemId: String,
    val showOnlyCollections: Boolean,
)

/**
 * Constructs a [VaultMoveToOrganizationArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVaultMoveToOrganizationArgs(): VaultMoveToOrganizationArgs {
    val route = this.toRoute<VaultMoveToOrganizationRoute>()
    return VaultMoveToOrganizationArgs(
        vaultItemId = route.vaultItemId,
        showOnlyCollections = route.showOnlyCollections,
    )
}

/**
 * Add the vault move to organization screen to the nav graph.
 */
fun NavGraphBuilder.vaultMoveToOrganizationDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<VaultMoveToOrganizationRoute> {
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
    showOnlyCollections: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = VaultMoveToOrganizationRoute(
            vaultItemId = vaultItemId,
            showOnlyCollections = showOnlyCollections,
        ),
        navOptions = navOptions,
    )
}

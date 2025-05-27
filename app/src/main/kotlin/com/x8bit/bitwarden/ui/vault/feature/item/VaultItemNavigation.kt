package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault item screen.
 */
@Serializable
data class VaultItemRoute(
    val vaultItemId: String,
    val cipherType: VaultItemCipherType,
)

/**
 * Class to retrieve vault item arguments from the [SavedStateHandle].
 */
data class VaultItemArgs(
    val vaultItemId: String,
    val cipherType: VaultItemCipherType,
)

/**
 * Constructs a [VaultItemArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVaultItemArgs(): VaultItemArgs {
    val route = this.toRoute<VaultItemRoute>()
    return VaultItemArgs(vaultItemId = route.vaultItemId, cipherType = route.cipherType)
}

/**
 * Add the vault item screen to the nav graph.
 */
fun NavGraphBuilder.vaultItemDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
    onNavigateToPasswordHistory: (vaultItemId: String) -> Unit,
) {
    composableWithSlideTransitions<VaultItemRoute> {
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
    args: VaultItemArgs,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = VaultItemRoute(
            vaultItemId = args.vaultItemId,
            cipherType = args.cipherType,
        ),
        navOptions = navOptions,
    )
}

package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithStayTransitions

/**
 * The functions below pertain to entry into the [VaultUnlockedNavBarScreen].
 */
const val VAULT_UNLOCKED_NAV_BAR_ROUTE: String = "VaultUnlockedNavBar"

/**
 * Navigate to the [VaultUnlockedNavBarScreen].
 */
fun NavController.navigateToVaultUnlockedNavBar(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_NAV_BAR_ROUTE, navOptions)
}

/**
 * Add vault unlocked destination to the root nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultUnlockedNavBarDestination(
    onNavigateToVaultAddItem: () -> Unit,
    onNavigateToVaultItem: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String) -> Unit,
    onNavigateToNewSend: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
) {
    composableWithStayTransitions(
        route = VAULT_UNLOCKED_NAV_BAR_ROUTE,
    ) {
        VaultUnlockedNavBarScreen(
            onNavigateToVaultAddItem = onNavigateToVaultAddItem,
            onNavigateToVaultItem = onNavigateToVaultItem,
            onNavigateToVaultEditItem = onNavigateToVaultEditItem,
            onNavigateToNewSend = onNavigateToNewSend,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
        )
    }
}

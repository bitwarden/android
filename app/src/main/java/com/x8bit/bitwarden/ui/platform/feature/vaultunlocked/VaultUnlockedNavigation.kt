package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.deleteAccountDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.navigateToDeleteAccount
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VAULT_UNLOCKED_NAV_BAR_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination
import com.x8bit.bitwarden.ui.tools.feature.send.navigateToNewSend
import com.x8bit.bitwarden.ui.tools.feature.send.newSendDestination
import com.x8bit.bitwarden.ui.vault.feature.vault.navigateToVaultAddItem
import com.x8bit.bitwarden.ui.vault.feature.vault.vaultAddItemDestination

const val VAULT_UNLOCKED_GRAPH_ROUTE: String = "vault_unlocked_graph"

/**
 * Navigate to the vault unlocked screen.
 */
fun NavController.navigateToVaultUnlockedGraph(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_GRAPH_ROUTE, navOptions)
}

/**
 * Add vault unlocked destinations to the root nav graph.
 */
fun NavGraphBuilder.vaultUnlockedGraph(
    navController: NavController,
) {
    navigation(
        startDestination = VAULT_UNLOCKED_NAV_BAR_ROUTE,
        route = VAULT_UNLOCKED_GRAPH_ROUTE,
    ) {
        vaultUnlockedNavBarDestination(
            onNavigateToVaultAddItem = { navController.navigateToVaultAddItem() },
            onNavigateToNewSend = { navController.navigateToNewSend() },
            onNavigateToDeleteAccount = { navController.navigateToDeleteAccount() },
        )
        deleteAccountDestination(onNavigateBack = { navController.popBackStack() })
        vaultAddItemDestination(onNavigateBack = { navController.popBackStack() })
        newSendDestination(onNavigateBack = { navController.popBackStack() })
    }
}

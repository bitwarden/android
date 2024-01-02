package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.deleteAccountDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.navigateToDeleteAccount
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.foldersDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.navigateToFolders
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VAULT_UNLOCKED_NAV_BAR_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.navigateToPasswordHistory
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.passwordHistoryDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.addSendDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.navigateToAddSend
import com.x8bit.bitwarden.ui.vault.feature.additem.navigateToVaultAddEditItem
import com.x8bit.bitwarden.ui.vault.feature.additem.vaultAddEditItemDestination
import com.x8bit.bitwarden.ui.vault.feature.item.navigateToVaultItem
import com.x8bit.bitwarden.ui.vault.feature.item.vaultItemDestination
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType

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
            onNavigateToFolders = { navController.navigateToFolders() },
            onNavigateToVaultAddItem = {
                navController.navigateToVaultAddEditItem(VaultAddEditType.AddItem)
            },
            onNavigateToVaultItem = { navController.navigateToVaultItem(it) },
            onNavigateToVaultEditItem = {
                navController.navigateToVaultAddEditItem(VaultAddEditType.EditItem(it))
            },
            onNavigateToAddSend = { navController.navigateToAddSend() },
            onNavigateToDeleteAccount = { navController.navigateToDeleteAccount() },
            onNavigateToPasswordHistory = { navController.navigateToPasswordHistory() },
        )
        deleteAccountDestination(onNavigateBack = { navController.popBackStack() })
        vaultAddEditItemDestination(onNavigateBack = { navController.popBackStack() })
        vaultItemDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultEditItem = {
                navController.navigateToVaultAddEditItem(VaultAddEditType.EditItem(it))
            },
        )
        addSendDestination(onNavigateBack = { navController.popBackStack() })
        passwordHistoryDestination(onNavigateBack = { navController.popBackStack() })
        foldersDestination(onNavigateBack = { navController.popBackStack() })
    }
}

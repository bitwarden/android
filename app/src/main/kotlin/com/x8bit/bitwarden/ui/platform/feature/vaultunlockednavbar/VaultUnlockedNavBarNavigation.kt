@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithStayTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault unlocked navbar screen.
 */
@Serializable
data object VaultUnlockedNavbarRoute

/**
 * Navigate to the [VaultUnlockedNavBarScreen].
 */
fun NavController.navigateToVaultUnlockedNavBar(navOptions: NavOptions? = null) {
    navigate(route = VaultUnlockedNavbarRoute, navOptions = navOptions)
}

/**
 * Add vault unlocked destination to the root nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultUnlockedNavBarDestination(
    onNavigateToVaultAddItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItem: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (ViewSendRoute) -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToSetupBrowserAutofill: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderName: String?) -> Unit,
    onNavigateToAboutPrivilegedApps: () -> Unit,
) {
    composableWithStayTransitions<VaultUnlockedNavbarRoute> {
        VaultUnlockedNavBarScreen(
            onNavigateToVaultAddItem = onNavigateToVaultAddItem,
            onNavigateToVaultItem = onNavigateToVaultItem,
            onNavigateToVaultEditItem = onNavigateToVaultEditItem,
            onNavigateToViewSend = onNavigateToViewSend,
            onNavigateToSearchSend = onNavigateToSearchSend,
            onNavigateToSearchVault = onNavigateToSearchVault,
            onNavigateToAddEditSend = onNavigateToAddEditSend,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToPendingRequests = onNavigateToPendingRequests,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
            onNavigateToSetupAutoFillScreen = onNavigateToSetupAutoFillScreen,
            onNavigateToSetupBrowserAutofill = onNavigateToSetupBrowserAutofill,
            onNavigateToImportLogins = onNavigateToImportLogins,
            onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
            onNavigateToFlightRecorder = onNavigateToFlightRecorder,
            onNavigateToRecordedLogs = onNavigateToRecordedLogs,
            onNavigateToAboutPrivilegedApps = onNavigateToAboutPrivilegedApps,
        )
    }
}

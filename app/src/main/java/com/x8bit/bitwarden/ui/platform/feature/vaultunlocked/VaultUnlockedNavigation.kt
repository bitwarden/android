package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupAutoFillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupUnlockScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupAutoFillDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupUnlockDestination
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.navigateToNewDeviceNoticeTwoFactor
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.newDeviceNoticeEmailAccessDestination
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.newDeviceNoticeTwoFactorDestination
import com.x8bit.bitwarden.ui.platform.feature.search.navigateToSearch
import com.x8bit.bitwarden.ui.platform.feature.search.searchDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.deleteAccountDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.navigateToDeleteAccount
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation.deleteAccountConfirmationDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation.navigateToDeleteAccountConfirmation
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.loginApprovalDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.navigateToLoginApproval
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests.navigateToPendingRequests
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests.pendingRequestsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.exportVaultDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.navigateToExportVault
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.folderAddEditDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.navigateToFolderAddEdit
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.foldersDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.navigateToFolders
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VAULT_UNLOCKED_NAV_BAR_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorModalDestination
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import com.x8bit.bitwarden.ui.tools.feature.generator.navigateToGeneratorModal
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.navigateToPasswordHistory
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.passwordHistoryDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.addSendDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.navigateToAddSend
import com.x8bit.bitwarden.ui.vault.feature.addedit.navigateToVaultAddEdit
import com.x8bit.bitwarden.ui.vault.feature.addedit.vaultAddEditDestination
import com.x8bit.bitwarden.ui.vault.feature.attachments.attachmentDestination
import com.x8bit.bitwarden.ui.vault.feature.attachments.navigateToAttachment
import com.x8bit.bitwarden.ui.vault.feature.importlogins.importLoginsScreenDestination
import com.x8bit.bitwarden.ui.vault.feature.importlogins.navigateToImportLoginsScreen
import com.x8bit.bitwarden.ui.vault.feature.item.navigateToVaultItem
import com.x8bit.bitwarden.ui.vault.feature.item.vaultItemDestination
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestinationAsRoot
import com.x8bit.bitwarden.ui.vault.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.x8bit.bitwarden.ui.vault.feature.manualcodeentry.vaultManualCodeEntryDestination
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.navigateToVaultMoveToOrganization
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.vaultMoveToOrganizationDestination
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.navigateToQrCodeScanScreen
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.vaultQrCodeScanDestination
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
@Suppress("LongMethod")
fun NavGraphBuilder.vaultUnlockedGraph(
    navController: NavController,
) {
    navigation(
        startDestination = VAULT_UNLOCKED_NAV_BAR_ROUTE,
        route = VAULT_UNLOCKED_GRAPH_ROUTE,
    ) {
        vaultItemListingDestinationAsRoot(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = { navController.navigateToVaultItem(vaultItemId = it) },
            onNavigateToVaultAddItemScreen = { cipherType, selectedFolderId, collectionId ->
                navController.navigateToVaultAddEdit(
                    VaultAddEditType.AddItem(cipherType),
                    selectedFolderId,
                    collectionId,
                )
            },
            onNavigateToSearchVault = { navController.navigateToSearch(searchType = it) },
            onNavigateToVaultEditItemScreen = {
                navController.navigateToVaultAddEdit(VaultAddEditType.EditItem(it))
            },
        )
        vaultUnlockedNavBarDestination(
            onNavigateToExportVault = { navController.navigateToExportVault() },
            onNavigateToFolders = { navController.navigateToFolders() },
            onNavigateToVaultAddItem = { cipherType, selectedFolderId, collectionId ->
                navController.navigateToVaultAddEdit(
                    VaultAddEditType.AddItem(cipherType),
                    selectedFolderId,
                    collectionId,
                )
            },
            onNavigateToVaultItem = { navController.navigateToVaultItem(it) },
            onNavigateToVaultEditItem = {
                navController.navigateToVaultAddEdit(VaultAddEditType.EditItem(it))
            },
            onNavigateToSearchVault = { navController.navigateToSearch(searchType = it) },
            onNavigateToSearchSend = { navController.navigateToSearch(searchType = it) },
            onNavigateToAddSend = { navController.navigateToAddSend(AddSendType.AddItem) },
            onNavigateToEditSend = { navController.navigateToAddSend(AddSendType.EditItem(it)) },
            onNavigateToDeleteAccount = { navController.navigateToDeleteAccount() },
            onNavigateToPendingRequests = { navController.navigateToPendingRequests() },
            onNavigateToPasswordHistory = {
                navController.navigateToPasswordHistory(
                    passwordHistoryMode = GeneratorPasswordHistoryMode.Default,
                )
            },
            onNavigateToSetupUnlockScreen = { navController.navigateToSetupUnlockScreen() },
            onNavigateToSetupAutoFillScreen = { navController.navigateToSetupAutoFillScreen() },
            onNavigateToImportLogins = {
                navController.navigateToImportLoginsScreen(snackbarRelay = it)
            },
        )
        deleteAccountDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDeleteAccountConfirmation = {
                navController.navigateToDeleteAccountConfirmation()
            },
        )
        deleteAccountConfirmationDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        loginApprovalDestination(onNavigateBack = { navController.popBackStack() })
        pendingRequestsDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLoginApproval = { navController.navigateToLoginApproval(it) },
        )
        vaultAddEditDestination(
            onNavigateToQrCodeScanScreen = {
                navController.navigateToQrCodeScanScreen()
            },
            onNavigateToManualCodeEntryScreen = {
                navController.navigateToManualCodeEntryScreen()
            },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToGeneratorModal = { navController.navigateToGeneratorModal(mode = it) },
            onNavigateToAttachments = { navController.navigateToAttachment(it) },
            onNavigateToMoveToOrganization = { vaultItemId, showOnlyCollections ->
                navController.navigateToVaultMoveToOrganization(
                    vaultItemId = vaultItemId,
                    showOnlyCollections = showOnlyCollections,
                )
            },
        )
        vaultMoveToOrganizationDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        vaultItemDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultEditItem = { vaultItemId, isClone ->
                navController.navigateToVaultAddEdit(
                    if (isClone) {
                        VaultAddEditType.CloneItem(vaultItemId)
                    } else {
                        VaultAddEditType.EditItem(vaultItemId)
                    },
                )
            },
            onNavigateToMoveToOrganization = { vaultItemId, showOnlyCollections ->
                navController.navigateToVaultMoveToOrganization(
                    vaultItemId = vaultItemId,
                    showOnlyCollections = showOnlyCollections,
                )
            },
            onNavigateToAttachments = { navController.navigateToAttachment(it) },
            onNavigateToPasswordHistory = {
                navController.navigateToPasswordHistory(
                    passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = it),
                )
            },
        )
        vaultQrCodeScanDestination(
            onNavigateToManualCodeEntryScreen = {
                navController.popBackStack()
                navController.navigateToManualCodeEntryScreen()
            },
            onNavigateBack = { navController.popBackStack() },
        )
        vaultManualCodeEntryDestination(
            onNavigateToQrCodeScreen = {
                navController.popBackStack()
                navController.navigateToQrCodeScanScreen()
            },
            onNavigateBack = { navController.popBackStack() },
        )

        addSendDestination(onNavigateBack = { navController.popBackStack() })
        passwordHistoryDestination(onNavigateBack = { navController.popBackStack() })
        exportVaultDestination(onNavigateBack = { navController.popBackStack() })
        foldersDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddFolderScreen = {
                navController.navigateToFolderAddEdit(FolderAddEditType.AddItem)
            },
            onNavigateToEditFolderScreen = {
                navController.navigateToFolderAddEdit(
                    FolderAddEditType.EditItem(it),
                )
            },
        )

        folderAddEditDestination(onNavigateBack = { navController.popBackStack() })
        generatorModalDestination(onNavigateBack = { navController.popBackStack() })
        searchDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEditSend = { navController.navigateToAddSend(AddSendType.EditItem(it)) },
            onNavigateToEditCipher = {
                navController.navigateToVaultAddEdit(VaultAddEditType.EditItem(it))
            },
            onNavigateToViewCipher = { navController.navigateToVaultItem(it) },
        )
        attachmentDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        setupUnlockDestination(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
        setupAutoFillDestination(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
        importLoginsScreenDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        newDeviceNoticeEmailAccessDestination(
            onNavigateBackToVault = { navController.navigateToVaultUnlockedGraph() },
            onNavigateToTwoFactorOptions = { navController.navigateToNewDeviceNoticeTwoFactor() },
        )
        newDeviceNoticeTwoFactorDestination(
            onNavigateBackToVault = { navController.navigateToVaultUnlockedGraph() },
        )
    }
}

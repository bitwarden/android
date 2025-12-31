@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupAutoFillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupBrowserAutofillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupUnlockScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupAutoFillDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupBrowserAutofillDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupUnlockDestination
import com.x8bit.bitwarden.ui.platform.feature.search.SearchRoute
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
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about.aboutPrivilegedAppsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about.navigateToAboutPrivilegedAppsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.exportVaultDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.navigateToExportVault
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.flightRecorderDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.navigateToFlightRecorder
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.navigateToRecordedLogs
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.recordedLogsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.folderAddEditDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.navigateToFolderAddEdit
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.foldersDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.navigateToFolders
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VaultUnlockedNavbarRoute
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorModalDestination
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import com.x8bit.bitwarden.ui.tools.feature.generator.navigateToGeneratorModal
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.navigateToPasswordHistory
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.passwordHistoryDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.addEditSendDestination
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.navigateToAddEditSend
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.navigateToViewSend
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.viewSendDestination
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
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.migrateToMyItemsDestination
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.navigateToVaultMoveToOrganization
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.vaultMoveToOrganizationDestination
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.navigateToQrCodeScanScreen
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.vaultQrCodeScanDestination
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault unlocked graph.
 */
@Serializable
data object VaultUnlockedGraphRoute

/**
 * Navigate to the vault unlocked screen.
 */
fun NavController.navigateToVaultUnlockedGraph(navOptions: NavOptions? = null) {
    navigate(route = VaultUnlockedGraphRoute, navOptions = navOptions)
}

/**
 * Add vault unlocked destinations to the root nav graph.
 */
@Suppress("LongMethod")
fun NavGraphBuilder.vaultUnlockedGraph(
    navController: NavController,
) {
    navigation<VaultUnlockedGraphRoute>(
        startDestination = VaultUnlockedNavbarRoute,
    ) {
        vaultItemListingDestinationAsRoot(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToVaultItemScreen = { navController.navigateToVaultItem(it) },
            onNavigateToVaultAddItemScreen = { navController.navigateToVaultAddEdit(it) },
            onNavigateToSearchVault = { navController.navigateToSearch(searchType = it) },
            onNavigateToVaultEditItemScreen = { navController.navigateToVaultAddEdit(it) },
            onNavigateToAddFolderScreen = {
                navController.navigateToFolderAddEdit(
                    folderAddEditType = FolderAddEditType.AddItem,
                    parentFolderName = it,
                )
            },
        )
        vaultUnlockedNavBarDestination(
            onNavigateToExportVault = { navController.navigateToExportVault() },
            onNavigateToFolders = { navController.navigateToFolders() },
            onNavigateToVaultAddItem = { navController.navigateToVaultAddEdit(it) },
            onNavigateToVaultItem = { navController.navigateToVaultItem(it) },
            onNavigateToVaultEditItem = { navController.navigateToVaultAddEdit(it) },
            onNavigateToSearchVault = { navController.navigateToSearch(searchType = it) },
            onNavigateToSearchSend = { navController.navigateToSearch(searchType = it) },
            onNavigateToAddEditSend = { navController.navigateToAddEditSend(it) },
            onNavigateToViewSend = { navController.navigateToViewSend(route = it) },
            onNavigateToDeleteAccount = { navController.navigateToDeleteAccount() },
            onNavigateToPendingRequests = { navController.navigateToPendingRequests() },
            onNavigateToPasswordHistory = {
                navController.navigateToPasswordHistory(
                    passwordHistoryMode = GeneratorPasswordHistoryMode.Default,
                )
            },
            onNavigateToSetupUnlockScreen = { navController.navigateToSetupUnlockScreen() },
            onNavigateToSetupAutoFillScreen = { navController.navigateToSetupAutoFillScreen() },
            onNavigateToSetupBrowserAutofill = {
                navController.navigateToSetupBrowserAutofillScreen()
            },
            onNavigateToImportLogins = { navController.navigateToImportLoginsScreen() },
            onNavigateToAddFolderScreen = {
                navController.navigateToFolderAddEdit(
                    folderAddEditType = FolderAddEditType.AddItem,
                    parentFolderName = it,
                )
            },
            onNavigateToFlightRecorder = {
                navController.navigateToFlightRecorder(isPreAuth = false)
            },
            onNavigateToRecordedLogs = { navController.navigateToRecordedLogs(isPreAuth = false) },
            onNavigateToAboutPrivilegedApps = {
                navController.navigateToAboutPrivilegedAppsScreen()
            },
        )
        flightRecorderDestination(
            isPreAuth = false,
            onNavigateBack = { navController.popBackStack() },
        )
        recordedLogsDestination(
            isPreAuth = false,
            onNavigateBack = { navController.popBackStack() },
        )
        aboutPrivilegedAppsDestination(
            onNavigateBack = { navController.popBackStack() },
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
            onNavigateToVaultEditItem = { navController.navigateToVaultAddEdit(it) },
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

        addEditSendDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateUpToSearchOrRoot = { navController.navigateUpToSearchOrVaultUnlockedRoot() },
        )
        viewSendDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddEditSend = { navController.navigateToAddEditSend(it) },
        )
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
            onNavigateToAddEditSend = { navController.navigateToAddEditSend(it) },
            onNavigateToViewSend = { navController.navigateToViewSend(it) },
            onNavigateToEditCipher = { navController.navigateToVaultAddEdit(it) },
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
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBrowserAutofill = { navController.navigateToSetupBrowserAutofillScreen() },
        )
        setupBrowserAutofillDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        importLoginsScreenDestination(
            onNavigateBack = { navController.popBackStack() },
        )

        migrateToMyItemsDestination(
            onNavigateToVault = { navController.popBackStack() },
            onNavigateToLeaveOrganization = { },
        )
    }
}

private fun NavController.navigateUpToSearchOrVaultUnlockedRoot() {
    if (!this.popBackStack<SearchRoute>(inclusive = false)) {
        this.navigateUpToVaultUnlockedRoot()
    }
}

private fun NavController.navigateUpToVaultUnlockedRoot() {
    this.popBackStack<VaultUnlockedNavbarRoute>(inclusive = false)
}

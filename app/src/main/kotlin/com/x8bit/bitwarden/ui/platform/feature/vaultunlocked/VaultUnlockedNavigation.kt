@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.popUpTo
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupBrowserAutofillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupAutoFillDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupBrowserAutofillDestination
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.setupUnlockDestination
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanMode
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.navigateToPlanModal
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.planModalDestination
import com.x8bit.bitwarden.ui.platform.feature.premium.upgraded.navigateToUpgradedToPremium
import com.x8bit.bitwarden.ui.platform.feature.premium.upgraded.upgradedToPremiumDestination
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph
import com.x8bit.bitwarden.ui.platform.feature.search.navigateToSearch
import com.x8bit.bitwarden.ui.platform.feature.search.searchDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.deleteAccountDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation.deleteAccountConfirmationDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation.navigateToDeleteAccountConfirmation
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.loginApprovalDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval.navigateToLoginApproval
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.manageDevicesDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests.pendingRequestsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about.aboutPrivilegedAppsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.exportVaultDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.flightRecorderDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.recordedLogsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.folderAddEditDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.navigateToFolderAddEdit
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.foldersDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
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
import com.x8bit.bitwarden.ui.vault.feature.attachments.preview.navigateToPreviewAttachment
import com.x8bit.bitwarden.ui.vault.feature.attachments.preview.previewAttachmentDestination
import com.x8bit.bitwarden.ui.vault.feature.cardscanner.cardScanDestination
import com.x8bit.bitwarden.ui.vault.feature.cardscanner.navigateToCardScanScreen
import com.x8bit.bitwarden.ui.vault.feature.importlogins.importLoginsScreenDestination
import com.x8bit.bitwarden.ui.vault.feature.item.navigateToVaultItem
import com.x8bit.bitwarden.ui.vault.feature.item.vaultItemDestination
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.vaultItemListingDestinationAsRoot
import com.x8bit.bitwarden.ui.vault.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.x8bit.bitwarden.ui.vault.feature.manualcodeentry.vaultManualCodeEntryDestination
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.navigateToVaultMoveToOrganization
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.vaultMoveToOrganizationDestination
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.navigateToQrCodeScanScreen
import com.x8bit.bitwarden.ui.vault.feature.qrcodescan.vaultQrCodeScanDestination

/**
 * Add the vault unlocked section to the root navigation entries. The
 * [RootNavGraph.VaultUnlockedRoute] hosts the [VaultUnlockedGraph].
 */
fun EntryProviderScope<RootNavGraph>.vaultUnlockedGraph() {
    entry<RootNavGraph.VaultUnlockedRoute> { route ->
        val navigationController = rememberNavigationController(
            routes = route.backStack.toTypedArray(),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                vaultUnlockedDestinations(navigationController = navigationController)
            },
        )
    }
}

/**
 * Add all the destinations available while the vault is unlocked to the vault unlocked navigation
 * entries. The [VaultUnlockedGraph.NavBarRoute] is the root of the standard vault unlocked back
 * stack.
 */
@Suppress("LongMethod")
private fun EntryProviderScope<VaultUnlockedGraph>.vaultUnlockedDestinations(
    navigationController: NavigationController<VaultUnlockedGraph>,
) {
    vaultItemListingDestinationAsRoot(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToVaultItemScreen = { navigationController.navigateToVaultItem(route = it) },
        onNavigateToVaultAddItemScreen = {
            navigationController.navigateToVaultAddEdit(route = it)
        },
        onNavigateToSearchVault = { navigationController.navigateToSearch(searchType = it) },
        onNavigateToVaultEditItemScreen = {
            navigationController.navigateToVaultAddEdit(route = it)
        },
        onNavigateToAddFolderScreen = {
            navigationController.navigateToFolderAddEdit(
                folderAddEditType = FolderAddEditType.AddItem,
                parentFolderName = it,
            )
        },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
    )
    vaultUnlockedNavBarDestination(navigationController = navigationController)
    flightRecorderDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    recordedLogsDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    aboutPrivilegedAppsDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    deleteAccountDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToDeleteAccountConfirmation = {
            navigationController.navigateToDeleteAccountConfirmation()
        },
    )
    deleteAccountConfirmationDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    loginApprovalDestination(onNavigateBack = { navigationController.navigateBack() })
    pendingRequestsDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToLoginApproval = {
            navigationController.navigateToLoginApproval(fingerprint = it)
        },
    )
    manageDevicesDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToLoginApproval = {
            navigationController.navigateToLoginApproval(fingerprint = it)
        },
    )
    vaultAddEditDestination(
        onNavigateToQrCodeScanScreen = { navigationController.navigateToQrCodeScanScreen() },
        onNavigateToCardScanScreen = { navigationController.navigateToCardScanScreen() },
        onNavigateToManualCodeEntryScreen = {
            navigationController.navigateToManualCodeEntryScreen()
        },
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToGeneratorModal = { navigationController.navigateToGeneratorModal(mode = it) },
        onNavigateToAttachments = { navigationController.navigateToAttachment(cipherId = it) },
        onNavigateToMoveToOrganization = { vaultItemId, showOnlyCollections ->
            navigationController.navigateToVaultMoveToOrganization(
                vaultItemId = vaultItemId,
                showOnlyCollections = showOnlyCollections,
            )
        },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
        onCloseAndNavigateToVaultItem = { cipherId, cipherType ->
            navigationController.navigatePoppingUpTo(
                route = VaultUnlockedGraph.VaultItemRoute(
                    vaultItemId = cipherId,
                    cipherType = cipherType,
                    showCreatedSnackbar = true,
                ),
                inclusive = true,
                predicate = { it is VaultUnlockedGraph.VaultAddEditRoute },
            )
        },
    )
    vaultMoveToOrganizationDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    vaultItemDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToVaultEditItem = { navigationController.navigateToVaultAddEdit(route = it) },
        onNavigateToMoveToOrganization = { vaultItemId, showOnlyCollections ->
            navigationController.navigateToVaultMoveToOrganization(
                vaultItemId = vaultItemId,
                showOnlyCollections = showOnlyCollections,
            )
        },
        onNavigateToAttachments = { navigationController.navigateToAttachment(cipherId = it) },
        onNavigateToPasswordHistory = {
            navigationController.navigateToPasswordHistory(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = it),
            )
        },
        onNavigateToPreviewAttachment = {
            navigationController.navigateToPreviewAttachment(route = it)
        },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
    )
    cardScanDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    vaultQrCodeScanDestination(
        onNavigateToManualCodeEntryScreen = {
            navigationController.replaceLast(route = VaultUnlockedGraph.ManualCodeEntryRoute)
        },
        onNavigateBack = { navigationController.navigateBack() },
    )
    vaultManualCodeEntryDestination(
        onNavigateToQrCodeScreen = {
            navigationController.replaceLast(route = VaultUnlockedGraph.QrCodeScanRoute)
        },
        onNavigateBack = { navigationController.navigateBack() },
    )

    addEditSendDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateUpToSearchOrRoot = {
            navigationController.navigateUpToSearchOrVaultUnlockedRoot()
        },
        onNavigateToGeneratorModal = { navigationController.navigateToGeneratorModal(mode = it) },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
    )
    viewSendDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToAddEditSend = { navigationController.navigateToAddEditSend(route = it) },
    )
    passwordHistoryDestination(onNavigateBack = { navigationController.navigateBack() })
    exportVaultDestination(onNavigateBack = { navigationController.navigateBack() })
    foldersDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToAddFolderScreen = {
            navigationController.navigateToFolderAddEdit(
                folderAddEditType = FolderAddEditType.AddItem,
            )
        },
        onNavigateToEditFolderScreen = {
            navigationController.navigateToFolderAddEdit(
                folderAddEditType = FolderAddEditType.EditItem(folderId = it),
            )
        },
    )

    folderAddEditDestination(onNavigateBack = { navigationController.navigateBack() })
    generatorModalDestination(onNavigateBack = { navigationController.navigateBack() })
    searchDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToAddEditSend = { navigationController.navigateToAddEditSend(route = it) },
        onNavigateToViewSend = { navigationController.navigateToViewSend(route = it) },
        onNavigateToEditCipher = { navigationController.navigateToVaultAddEdit(route = it) },
        onNavigateToViewCipher = { navigationController.navigateToVaultItem(route = it) },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
    )
    attachmentDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToPreviewAttachment = {
            navigationController.navigateToPreviewAttachment(route = it)
        },
        onNavigateToPlan = { navigationController.navigateToPlanModal() },
    )
    setupUnlockDestination(
        onNavigateBack = {
            navigationController.navigateBack()
        },
    )
    setupAutoFillDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToBrowserAutofill = {
            navigationController.navigateToSetupBrowserAutofillScreen()
        },
    )
    setupBrowserAutofillDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    importLoginsScreenDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    previewAttachmentDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    planModalDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToUpgradedToPremium = {
            navigationController.navigateToUpgradedToPremium(planMode = PlanMode.Modal)
        },
    )
    upgradedToPremiumDestination(
        onDismiss = { planMode ->
            when (planMode) {
                PlanMode.Standard -> navigationController.navigateBack()
                PlanMode.Modal -> {
                    navigationController.popUpTo<VaultUnlockedGraph.PlanModalRoute>(
                        inclusive = true,
                    )
                }
            }
        },
    )
}

private fun NavigationController<VaultUnlockedGraph>.navigateUpToSearchOrVaultUnlockedRoot() {
    if (!this.popUpTo<VaultUnlockedGraph.SearchRoute>()) {
        this.popUpTo<VaultUnlockedGraph.NavBarRoute>()
    }
}

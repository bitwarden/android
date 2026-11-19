@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenTabNavDisplay
import com.bitwarden.ui.platform.navigation.util.popUpTo
import com.bitwarden.ui.platform.navigation.util.rememberNavKeyEntries
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.stayEntry
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupAutoFillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupBrowserAutofillScreen
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.navigateToSetupUnlockScreen
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanMode
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.navigateToPlanModal
import com.x8bit.bitwarden.ui.platform.feature.premium.upgraded.navigateToUpgradedToPremium
import com.x8bit.bitwarden.ui.platform.feature.search.navigateToSearch
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.navigateToDeleteAccount
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.navigateToManageDevices
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests.navigateToPendingRequests
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.navigateToAutoFill
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about.navigateToAboutPrivilegedAppsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.navigateToExportVault
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.navigateToFlightRecorder
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.navigateToRecordedLogs
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit.navigateToFolderAddEdit
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.navigateToFolders
import com.x8bit.bitwarden.ui.platform.feature.settings.settingsDestinations
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorDestinations
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.navigateToPasswordHistory
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.navigateToAddEditSend
import com.x8bit.bitwarden.ui.tools.feature.send.sendDestinations
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.navigateToViewSend
import com.x8bit.bitwarden.ui.vault.feature.addedit.navigateToVaultAddEdit
import com.x8bit.bitwarden.ui.vault.feature.importlogins.navigateToImportLoginsScreen
import com.x8bit.bitwarden.ui.vault.feature.item.navigateToVaultItem
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.vaultDestinations

/**
 * Add the vault unlocked navigation bar to the vault unlocked navigation entries. The
 * [VaultUnlockedGraph.NavBarRoute] hosts the [VaultUnlockedNavBarGraph], where each tab has its
 * own back stack within its own navigation graph.
 *
 * @param navigationController The [NavigationController] of the [VaultUnlockedGraph], which
 * displays any screen that is presented above the navigation bar.
 */
fun EntryProviderScope<VaultUnlockedGraph>.vaultUnlockedNavBarDestination(
    navigationController: NavigationController<VaultUnlockedGraph>,
) {
    stayEntry<VaultUnlockedGraph.NavBarRoute> {
        VaultUnlockedNavBarNavDisplay(vaultUnlockedNavigationController = navigationController)
    }
}

@Composable
private fun VaultUnlockedNavBarNavDisplay(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
) {
    val navigationController = rememberNavigationController<VaultUnlockedNavBarGraph>(
        routes = arrayOf(VaultUnlockedNavBarGraph.Vault),
    )
    val tabNavigationControllers = rememberVaultUnlockedNavBarNavigationControllers(
        navBarNavigationController = navigationController,
    )
    VaultUnlockedNavBarScreen(
        bottomNavigationController = tabNavigationControllers,
    ) { onDimNavBarRequest ->
        val tabEntries = tabNavigationControllers.rememberTabNavEntries(
            vaultUnlockedNavigationController = vaultUnlockedNavigationController,
            onDimNavBarRequest = onDimNavBarRequest,
        )
        BitwardenTabNavDisplay(
            bottomNavigationController = tabNavigationControllers,
            tabEntries = tabEntries,
        )
    }
}

@Composable
private fun VaultUnlockedNavBarNavigationControllers.rememberTabNavEntries(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
    onDimNavBarRequest: (shouldDim: Boolean) -> Unit,
): Map<VaultUnlockedNavBarGraph, List<NavEntry<NavKey>>> = mapOf(
    VaultUnlockedNavBarGraph.Vault to vault.rememberNavKeyEntries(
        entryProvider = vaultEntryProvider(
            vaultUnlockedNavigationController = vaultUnlockedNavigationController,
            onDimNavBarRequest = onDimNavBarRequest,
        ),
    ),
    VaultUnlockedNavBarGraph.Send to send.rememberNavKeyEntries(
        entryProvider = sendEntryProvider(
            vaultUnlockedNavigationController = vaultUnlockedNavigationController,
        ),
    ),
    VaultUnlockedNavBarGraph.Generator to generator.rememberNavKeyEntries(
        entryProvider = generatorEntryProvider(
            vaultUnlockedNavigationController = vaultUnlockedNavigationController,
            onDimNavBarRequest = onDimNavBarRequest,
        ),
    ),
    VaultUnlockedNavBarGraph.Settings to settings.rememberNavKeyEntries(
        entryProvider = settingsEntryProvider(
            vaultUnlockedNavigationController = vaultUnlockedNavigationController,
        ),
    ),
)

private fun VaultUnlockedNavBarNavigationControllers.vaultEntryProvider(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
    onDimNavBarRequest: (shouldDim: Boolean) -> Unit,
): (key: VaultGraph) -> NavEntry<VaultGraph> = entryProvider {
    vaultDestinations(
        navigationController = vault,
        onNavigateToVaultAddItemScreen = {
            vaultUnlockedNavigationController.navigateToVaultAddEdit(route = it)
        },
        onNavigateToVaultItemScreen = {
            vaultUnlockedNavigationController.navigateToVaultItem(route = it)
        },
        onNavigateToVaultEditItemScreen = {
            vaultUnlockedNavigationController.navigateToVaultAddEdit(route = it)
        },
        onNavigateToSearchVault = {
            vaultUnlockedNavigationController.navigateToSearch(searchType = it)
        },
        onDimBottomNavBarRequest = onDimNavBarRequest,
        onNavigateToImportLogins = {
            vaultUnlockedNavigationController.navigateToImportLoginsScreen()
        },
        onNavigateToAddFolderScreen = {
            vaultUnlockedNavigationController.navigateToFolderAddEdit(
                folderAddEditType = FolderAddEditType.AddItem,
                parentFolderName = it,
            )
        },
        onNavigateToAboutScreen = {
            root.navigateToTopLevelRoute(route = VaultUnlockedNavBarGraph.Settings)
            settings.popUpTo<SettingsGraph.SettingsRoute>()
            settings.navigateToAbout()
        },
        onNavigateToAutofillScreen = {
            root.navigateToTopLevelRoute(route = VaultUnlockedNavBarGraph.Settings)
            settings.popUpTo<SettingsGraph.SettingsRoute>()
            settings.navigateToAutoFill()
        },
        onNavigateToPlan = { vaultUnlockedNavigationController.navigateToPlanModal() },
    )
}

private fun VaultUnlockedNavBarNavigationControllers.sendEntryProvider(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
): (key: SendGraph) -> NavEntry<SendGraph> = entryProvider {
    sendDestinations(
        navigationController = send,
        onNavigateToAddEditSend = {
            vaultUnlockedNavigationController.navigateToAddEditSend(route = it)
        },
        onNavigateToViewSend = {
            vaultUnlockedNavigationController.navigateToViewSend(route = it)
        },
        onNavigateToSearchSend = {
            vaultUnlockedNavigationController.navigateToSearch(searchType = it)
        },
        onNavigateToPlan = { vaultUnlockedNavigationController.navigateToPlanModal() },
    )
}

private fun generatorEntryProvider(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
    onDimNavBarRequest: (shouldDim: Boolean) -> Unit,
): (key: GeneratorGraph) -> NavEntry<GeneratorGraph> = entryProvider {
    generatorDestinations(
        navigateToVaultAddItem = {
            vaultUnlockedNavigationController.navigateToVaultAddEdit(route = it)
        },
        onNavigateToPasswordHistory = {
            vaultUnlockedNavigationController.navigateToPasswordHistory(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Default,
            )
        },
        onDimNavBarRequest = onDimNavBarRequest,
    )
}

private fun VaultUnlockedNavBarNavigationControllers.settingsEntryProvider(
    vaultUnlockedNavigationController: NavigationController<VaultUnlockedGraph>,
): (key: SettingsGraph) -> NavEntry<SettingsGraph> = entryProvider {
    settingsDestinations(
        navigationController = settings,
        onNavigateToDeleteAccount = {
            vaultUnlockedNavigationController.navigateToDeleteAccount()
        },
        onNavigateToExportVault = { vaultUnlockedNavigationController.navigateToExportVault() },
        onNavigateToFolders = { vaultUnlockedNavigationController.navigateToFolders() },
        onNavigateToPendingRequests = {
            vaultUnlockedNavigationController.navigateToPendingRequests()
        },
        onNavigateToSetupUnlockScreen = {
            vaultUnlockedNavigationController.navigateToSetupUnlockScreen()
        },
        onNavigateToSetupAutoFillScreen = {
            vaultUnlockedNavigationController.navigateToSetupAutoFillScreen()
        },
        onNavigateToSetupBrowserAutofill = {
            vaultUnlockedNavigationController.navigateToSetupBrowserAutofillScreen()
        },
        onNavigateToFlightRecorder = {
            vaultUnlockedNavigationController.navigateToFlightRecorder()
        },
        onNavigateToRecordedLogs = {
            vaultUnlockedNavigationController.navigateToRecordedLogs()
        },
        onNavigateToImportLogins = {
            vaultUnlockedNavigationController.navigateToImportLoginsScreen()
        },
        onNavigateToAboutPrivilegedApps = {
            vaultUnlockedNavigationController.navigateToAboutPrivilegedAppsScreen()
        },
        onNavigateToManageDevices = {
            vaultUnlockedNavigationController.navigateToManageDevices()
        },
        onNavigateToUpgradedToPremium = {
            vaultUnlockedNavigationController.navigateToUpgradedToPremium(
                planMode = PlanMode.Standard,
            )
        },
    )
}

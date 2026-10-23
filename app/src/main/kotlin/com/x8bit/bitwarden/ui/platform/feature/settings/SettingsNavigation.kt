@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootPushEntry
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.navigateToPlan
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.planDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.about.aboutDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToPreAuthAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.about.preAuthAboutDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.accountSecurityDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.navigateToAccountSecurity
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.appearanceDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.navigateToAppearance
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.navigateToPreAuthAppearance
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.preAuthAppearanceDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.autoFillDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.blockAutoFillDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.navigateToBlockAutoFillScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.navigateToAutoFill
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.navigateToPrivilegedAppsList
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.privilegedAppsListDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.navigateToPreAuthFlightRecorder
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.preAuthFlightRecorderDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.navigateToPreAuthRecordedLogs
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.preAuthRecordedLogsDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.other.navigateToOther
import com.x8bit.bitwarden.ui.platform.feature.settings.other.navigateToPreAuthOther
import com.x8bit.bitwarden.ui.platform.feature.settings.other.otherDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.other.preAuthOtherDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.navigateToVaultSettings
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.vaultSettingsDestination
import com.x8bit.bitwarden.ui.vault.feature.importitems.importItemsDestination
import com.x8bit.bitwarden.ui.vault.feature.importitems.navigateToImportItemsScreen

/**
 * Add the settings screen and its associated screens to the settings navigation entries. The
 * [SettingsGraph.SettingsRoute] is the root of the settings tab back stack.
 */
@Suppress("LongMethod", "LongParameterList")
fun EntryProviderScope<SettingsGraph>.settingsDestinations(
    navigationController: NavigationController<SettingsGraph>,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToSetupBrowserAutofill: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAboutPrivilegedApps: () -> Unit,
    onNavigateToManageDevices: () -> Unit,
    onNavigateToUpgradedToPremium: () -> Unit,
) {
    rootPushEntry<SettingsGraph.SettingsRoute> {
        SettingsScreen(
            viewModel = settingsViewModel(isPreAuth = false),
            onNavigateBack = {},
            onNavigateToAbout = { navigationController.navigateToAbout() },
            onNavigateToAccountSecurity = { navigationController.navigateToAccountSecurity() },
            onNavigateToAppearance = { navigationController.navigateToAppearance() },
            onNavigateToAutoFill = { navigationController.navigateToAutoFill() },
            onNavigateToOther = { navigationController.navigateToOther() },
            onNavigateToVault = { navigationController.navigateToVaultSettings() },
            onNavigateToPlan = { navigationController.navigateToPlan() },
        )
    }
    aboutDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToFlightRecorder = onNavigateToFlightRecorder,
        onNavigateToRecordedLogs = onNavigateToRecordedLogs,
    )
    accountSecurityDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToDeleteAccount = onNavigateToDeleteAccount,
        onNavigateToPendingRequests = onNavigateToPendingRequests,
        onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
        onNavigateToManageDevices = onNavigateToManageDevices,
    )
    appearanceDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    autoFillDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToBlockAutoFillScreen = { navigationController.navigateToBlockAutoFillScreen() },
        onNavigateToSetupAutofill = onNavigateToSetupAutoFillScreen,
        onNavigateToSetupBrowserAutofill = onNavigateToSetupBrowserAutofill,
        onNavigateToAboutPrivilegedAppsScreen = onNavigateToAboutPrivilegedApps,
        onNavigateToPrivilegedAppsList = { navigationController.navigateToPrivilegedAppsList() },
    )
    otherDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
    vaultSettingsDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToExportVault = onNavigateToExportVault,
        onNavigateToFolders = onNavigateToFolders,
        onNavigateToImportLogins = onNavigateToImportLogins,
        onNavigateToImportItems = { navigationController.navigateToImportItemsScreen() },
    )
    importItemsDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToImportLogins = onNavigateToImportLogins,
    )
    blockAutoFillDestination(onNavigateBack = { navigationController.navigateBack() })
    privilegedAppsListDestination(onNavigateBack = { navigationController.navigateBack() })
    planDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToUpgradedToPremium = onNavigateToUpgradedToPremium,
    )
}

/**
 * Add the pre-authentication settings to the auth navigation entries. The
 * [AuthGraph.PreAuthSettingsRoute] hosts the [PreAuthSettingsGraph].
 */
fun EntryProviderScope<AuthGraph>.preAuthSettingsDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<AuthGraph.PreAuthSettingsRoute> {
        val navigationController = rememberNavigationController<PreAuthSettingsGraph>(
            routes = arrayOf(PreAuthSettingsGraph.SettingsRoute),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                entry<PreAuthSettingsGraph.SettingsRoute> {
                    SettingsScreen(
                        viewModel = settingsViewModel(isPreAuth = true),
                        onNavigateBack = onNavigateBack,
                        onNavigateToAbout = { navigationController.navigateToPreAuthAbout() },
                        onNavigateToAppearance = {
                            navigationController.navigateToPreAuthAppearance()
                        },
                        onNavigateToOther = { navigationController.navigateToPreAuthOther() },
                        onNavigateToAccountSecurity = { /* no-op */ },
                        onNavigateToAutoFill = { /* no-op */ },
                        onNavigateToVault = { /* no-op */ },
                        onNavigateToPlan = { /* no-op */ },
                    )
                }
                preAuthAppearanceDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
                preAuthOtherDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
                preAuthAboutDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                    onNavigateToFlightRecorder = {
                        navigationController.navigateToPreAuthFlightRecorder()
                    },
                    onNavigateToRecordedLogs = {
                        navigationController.navigateToPreAuthRecordedLogs()
                    },
                )
                preAuthFlightRecorderDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
                preAuthRecordedLogsDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
            },
        )
    }
}

@Composable
private fun settingsViewModel(
    isPreAuth: Boolean,
): SettingsViewModel = hiltViewModel<SettingsViewModel, SettingsViewModel.Factory>(
    creationCallback = { factory -> factory.create(isPreAuth = isPreAuth) },
)

/**
 * Navigate to the pre-authentication settings.
 */
fun NavigationController<AuthGraph>.navigateToPreAuthSettings() {
    this.navigate(route = AuthGraph.PreAuthSettingsRoute)
}

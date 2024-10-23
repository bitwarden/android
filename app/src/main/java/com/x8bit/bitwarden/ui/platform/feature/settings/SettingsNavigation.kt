package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import com.x8bit.bitwarden.ui.platform.feature.settings.about.aboutDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.accountSecurityDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.navigateToAccountSecurity
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.appearanceDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.navigateToAppearance
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.autoFillDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.blockAutoFillDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.navigateToBlockAutoFillScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.navigateToAutoFill
import com.x8bit.bitwarden.ui.platform.feature.settings.other.navigateToOther
import com.x8bit.bitwarden.ui.platform.feature.settings.other.otherDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.navigateToVaultSettings
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.vaultSettingsDestination
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay

const val SETTINGS_GRAPH_ROUTE: String = "settings_graph"
private const val SETTINGS_ROUTE: String = "settings"

/**
 * Add settings destinations to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    navigation(
        startDestination = SETTINGS_ROUTE,
        route = SETTINGS_GRAPH_ROUTE,
    ) {
        composableWithRootPushTransitions(
            route = SETTINGS_ROUTE,
        ) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigateToAbout() },
                onNavigateToAccountSecurity = { navController.navigateToAccountSecurity() },
                onNavigateToAppearance = { navController.navigateToAppearance() },
                onNavigateToAutoFill = { navController.navigateToAutoFill() },
                onNavigateToOther = { navController.navigateToOther() },
                onNavigateToVault = { navController.navigateToVaultSettings() },
            )
        }
        aboutDestination(onNavigateBack = { navController.popBackStack() })
        accountSecurityDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToPendingRequests = onNavigateToPendingRequests,
            onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
        )
        appearanceDestination(onNavigateBack = { navController.popBackStack() })
        autoFillDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBlockAutoFillScreen = { navController.navigateToBlockAutoFillScreen() },
            onNavigateToSetupAutofill = onNavigateToSetupAutoFillScreen,
        )
        otherDestination(onNavigateBack = { navController.popBackStack() })
        vaultSettingsDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToExportVault = onNavigateToExportVault,
            onNavigateToFolders = onNavigateToFolders,
            onNavigateToImportLogins = onNavigateToImportLogins,
        )
        blockAutoFillDestination(onNavigateBack = { navController.popBackStack() })
    }
}

/**
 * Navigate to the settings screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(SETTINGS_GRAPH_ROUTE, navOptions)
}

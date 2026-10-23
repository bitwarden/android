package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the account security screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.accountSecurityDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToManageDevices: () -> Unit,
) {
    pushEntry<SettingsGraph.AccountSecurityRoute> {
        AccountSecurityScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToPendingRequests = onNavigateToPendingRequests,
            onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
            onNavigateToManageDevices = onNavigateToManageDevices,
        )
    }
}

/**
 * Navigate to the account security screen.
 */
fun NavigationController<SettingsGraph>.navigateToAccountSecurity() {
    this.navigate(route = SettingsGraph.AccountSecurityRoute)
}

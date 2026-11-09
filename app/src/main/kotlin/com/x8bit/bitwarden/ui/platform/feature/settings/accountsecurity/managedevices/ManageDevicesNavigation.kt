package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the manage devices screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.manageDevicesDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprintPhrase: String) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.ManageDevicesRoute> {
        ManageDevicesScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLoginApproval = onNavigateToLoginApproval,
        )
    }
}

/**
 * Navigate to the Manage Devices screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToManageDevices() {
    this.navigate(route = VaultUnlockedGraph.ManageDevicesRoute)
}

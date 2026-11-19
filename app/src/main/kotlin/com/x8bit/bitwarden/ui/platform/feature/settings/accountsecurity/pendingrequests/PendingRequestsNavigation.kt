package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the pending requests screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.pendingRequestsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprintPhrase: String) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.PendingRequestsRoute> {
        PendingRequestsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLoginApproval = onNavigateToLoginApproval,
        )
    }
}

/**
 * Navigate to the Pending Login Requests screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToPendingRequests() {
    this.navigate(route = VaultUnlockedGraph.PendingRequestsRoute)
}

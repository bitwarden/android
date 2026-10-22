@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the about privileged apps screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.aboutPrivilegedAppsDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.AboutPrivilegedAppsRoute> {
        AboutPrivilegedAppsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the about privileged apps screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToAboutPrivilegedAppsScreen() {
    this.navigate(route = VaultUnlockedGraph.AboutPrivilegedAppsRoute)
}

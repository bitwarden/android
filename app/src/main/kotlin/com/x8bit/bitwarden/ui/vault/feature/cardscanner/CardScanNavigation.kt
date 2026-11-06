@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the card scan screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.cardScanDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.CardScanRoute> {
        CardScanScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the card scan screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToCardScanScreen() {
    this.navigate(route = VaultUnlockedGraph.CardScanRoute)
}

package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the QR code scan screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.vaultQrCodeScanDestination(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.QrCodeScanRoute> {
        QrCodeScanScreen(
            onNavigateToManualCodeEntryScreen = onNavigateToManualCodeEntryScreen,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the QR code scan screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToQrCodeScanScreen() {
    this.navigate(route = VaultUnlockedGraph.QrCodeScanRoute)
}

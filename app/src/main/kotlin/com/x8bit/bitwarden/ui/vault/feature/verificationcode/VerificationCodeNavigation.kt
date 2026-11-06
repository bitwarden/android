package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph

/**
 * Add the verification code screen to the navigation entries.
 */
fun EntryProviderScope<VaultGraph>.vaultVerificationCodeDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSearchVault: () -> Unit,
    onNavigateToVaultItemScreen: (route: VaultUnlockedGraph.VaultItemRoute) -> Unit,
) {
    pushEntry<VaultGraph.VerificationCodeRoute> {
        VerificationCodeScreen(
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToSearch = onNavigateToSearchVault,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the verification code screen.
 */
fun NavigationController<VaultGraph>.navigateToVerificationCodeScreen() {
    this.navigate(route = VaultGraph.VerificationCodeRoute)
}

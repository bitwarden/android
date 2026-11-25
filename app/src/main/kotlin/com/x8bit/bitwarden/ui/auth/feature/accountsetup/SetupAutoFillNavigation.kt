package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Navigate to the setup autofill screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToSetupAutoFillScreen() {
    this.navigate(route = VaultUnlockedGraph.SetupAutofillRoute)
}

/**
 * Add the setup autofill screen to the vault unlocked navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.setupAutoFillDestination(
    onNavigateBack: () -> Unit,
    onNavigateToBrowserAutofill: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.SetupAutofillRoute> {
        SetupAutoFillScreen(
            viewModel = setupAutoFillViewModel(isInitialSetup = false),
            onNavigateBack = onNavigateBack,
            onNavigateToBrowserAutofill = {
                onNavigateBack()
                onNavigateToBrowserAutofill()
            },
        )
    }
}

/**
 * Add the setup autofill screen to the account setup navigation entries.
 */
fun EntryProviderScope<AccountSetupGraph>.setupAutoFillDestinationAsRoot() {
    entry<AccountSetupGraph.SetupAutofillRoute> {
        SetupAutoFillScreen(
            viewModel = setupAutoFillViewModel(isInitialSetup = true),
            onNavigateBack = {
                // No-Op
            },
            onNavigateToBrowserAutofill = {
                // No-Op
            },
        )
    }
}

@Composable
private fun setupAutoFillViewModel(
    isInitialSetup: Boolean,
): SetupAutoFillViewModel = hiltViewModel<SetupAutoFillViewModel, SetupAutoFillViewModel.Factory>(
    creationCallback = { factory -> factory.create(isInitialSetup = isInitialSetup) },
)

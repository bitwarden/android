package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Navigate to the setup browser autofill screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToSetupBrowserAutofillScreen() {
    this.navigate(route = VaultUnlockedGraph.SetupBrowserAutofillRoute)
}

/**
 * Add the setup browser autofill screen to the vault unlocked navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.setupBrowserAutofillDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.SetupBrowserAutofillRoute> {
        SetupBrowserAutofillScreen(
            viewModel = setupBrowserAutofillViewModel(isInitialSetup = false),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Add the setup browser autofill screen to the account setup navigation entries.
 */
fun EntryProviderScope<AccountSetupGraph>.setupBrowserAutofillDestinationAsRoot() {
    entry<AccountSetupGraph.SetupBrowserAutofillRoute> {
        SetupBrowserAutofillScreen(
            viewModel = setupBrowserAutofillViewModel(isInitialSetup = true),
            onNavigateBack = {
                // No-Op
            },
        )
    }
}

@Composable
private fun setupBrowserAutofillViewModel(
    isInitialSetup: Boolean,
): SetupBrowserAutofillViewModel =
    hiltViewModel<SetupBrowserAutofillViewModel, SetupBrowserAutofillViewModel.Factory>(
        creationCallback = { factory -> factory.create(isInitialSetup = isInitialSetup) },
    )

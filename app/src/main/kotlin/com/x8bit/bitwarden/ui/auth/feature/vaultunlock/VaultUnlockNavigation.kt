package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType

/**
 * Add the Vault Unlock screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.vaultUnlockDestination() {
    entry<AuthGraph.VaultUnlockRoute> { route ->
        VaultUnlockScreen(viewModel = vaultUnlockViewModel(route = route))
    }
}

/**
 * Navigate to the Vault Unlock screen for a given [unlockType].
 */
fun NavigationController<AuthGraph>.navigateToVaultUnlock(unlockType: UnlockType) {
    this.navigate(route = AuthGraph.VaultUnlockRoute(unlockType = unlockType))
}

/**
 * Creates the [VaultUnlockViewModel] for the given [route].
 */
@Composable
private fun vaultUnlockViewModel(
    route: AuthGraph.VaultUnlockRoute,
): VaultUnlockViewModel = hiltViewModel<VaultUnlockViewModel, VaultUnlockViewModel.Factory>(
    creationCallback = { factory -> factory.create(route = route) },
)

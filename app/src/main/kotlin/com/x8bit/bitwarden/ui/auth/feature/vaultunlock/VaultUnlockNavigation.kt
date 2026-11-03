package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Add the Vault Unlock screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.vaultUnlockDestination() {
    entry<AuthGraph.VaultUnlockRoute.Standard> { route ->
        VaultUnlockScreen(viewModel = vaultUnlockViewModel(route = route))
    }
}

/**
 * Navigate to the Vault Unlock screen for TDE.
 */
fun NavigationController<AuthGraph>.navigateToTdeVaultUnlock() {
    this.navigate(route = AuthGraph.VaultUnlockRoute.Tde)
}

/**
 * Add the Vault Unlock screen for TDE to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.tdeVaultUnlockDestination() {
    entry<AuthGraph.VaultUnlockRoute.Tde> { route ->
        VaultUnlockScreen(viewModel = vaultUnlockViewModel(route = route))
    }
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

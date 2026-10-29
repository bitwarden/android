package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode

/**
 * Add password history destination to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.passwordHistoryDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.PasswordHistoryRoute> { route ->
        PasswordHistoryScreen(
            viewModel = hiltViewModel<PasswordHistoryViewModel, PasswordHistoryViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Password History Screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToPasswordHistory(
    passwordHistoryMode: GeneratorPasswordHistoryMode,
) {
    this.navigate(
        route = VaultUnlockedGraph.PasswordHistoryRoute(passwordHistoryMode = passwordHistoryMode),
    )
}

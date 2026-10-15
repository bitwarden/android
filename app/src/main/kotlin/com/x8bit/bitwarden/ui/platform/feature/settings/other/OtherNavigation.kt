package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.PreAuthSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the other screen to the settings navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.otherDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<SettingsGraph.OtherRoute> {
        OtherScreen(
            viewModel = otherViewModel(isPreAuth = false),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Add the other screen to the pre-authentication settings navigation entries.
 */
fun EntryProviderScope<PreAuthSettingsGraph>.preAuthOtherDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<PreAuthSettingsGraph.OtherRoute> {
        OtherScreen(
            viewModel = otherViewModel(isPreAuth = true),
            onNavigateBack = onNavigateBack,
        )
    }
}

@Composable
private fun otherViewModel(
    isPreAuth: Boolean,
): OtherViewModel = hiltViewModel<OtherViewModel, OtherViewModel.Factory>(
    creationCallback = { factory -> factory.create(isPreAuth = isPreAuth) },
)

/**
 * Navigate to the other screen.
 */
fun NavigationController<SettingsGraph>.navigateToOther() {
    this.navigate(route = SettingsGraph.OtherRoute)
}

/**
 * Navigate to the pre-authentication other screen.
 */
fun NavigationController<PreAuthSettingsGraph>.navigateToPreAuthOther() {
    this.navigate(route = PreAuthSettingsGraph.OtherRoute)
}

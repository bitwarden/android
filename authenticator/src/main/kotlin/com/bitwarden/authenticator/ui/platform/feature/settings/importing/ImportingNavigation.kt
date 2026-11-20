package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry

/**
 * Add the import screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.importingDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<SettingsGraph.ImportRoute> {
        ImportingScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the Import destination.
 */
fun NavigationController<SettingsGraph>.navigateToImporting() {
    this.navigate(route = SettingsGraph.ImportRoute)
}

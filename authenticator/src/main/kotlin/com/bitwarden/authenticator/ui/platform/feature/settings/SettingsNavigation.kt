package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.util.rootPushEntry

/**
 * Add settings destination to the settings navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.settingsDestination(
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTutorial: () -> Unit,
) {
    rootPushEntry<SettingsGraph.SettingsRoute> {
        SettingsScreen(
            onNavigateToTutorial = onNavigateToTutorial,
            onNavigateToExport = onNavigateToExport,
            onNavigateToImport = onNavigateToImport,
        )
    }
}

package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.PreAuthSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the about screen to the settings navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.aboutDestination(
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
) {
    pushEntry<SettingsGraph.AboutRoute> {
        AboutScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToFlightRecorder = onNavigateToFlightRecorder,
            onNavigateToRecordedLogs = onNavigateToRecordedLogs,
        )
    }
}

/**
 * Add the about screen to the pre-authentication settings navigation entries.
 */
fun EntryProviderScope<PreAuthSettingsGraph>.preAuthAboutDestination(
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
) {
    pushEntry<PreAuthSettingsGraph.AboutRoute> {
        AboutScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToFlightRecorder = onNavigateToFlightRecorder,
            onNavigateToRecordedLogs = onNavigateToRecordedLogs,
        )
    }
}

/**
 * Navigate to the about screen.
 */
fun NavigationController<SettingsGraph>.navigateToAbout() {
    this.navigate(route = SettingsGraph.AboutRoute)
}

/**
 * Navigate to the pre-authentication about screen.
 */
fun NavigationController<PreAuthSettingsGraph>.navigateToPreAuthAbout() {
    this.navigate(route = PreAuthSettingsGraph.AboutRoute)
}

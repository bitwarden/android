package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.PreAuthSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the flight recorder screen to the vault unlocked navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.flightRecorderDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.FlightRecorderRoute> {
        FlightRecorderScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Add the flight recorder screen to the pre-authentication settings navigation entries.
 */
fun EntryProviderScope<PreAuthSettingsGraph>.preAuthFlightRecorderDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<PreAuthSettingsGraph.FlightRecorderRoute> {
        FlightRecorderScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the flight recorder screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToFlightRecorder() {
    this.navigate(route = VaultUnlockedGraph.FlightRecorderRoute)
}

/**
 * Navigate to the pre-authentication flight recorder screen.
 */
fun NavigationController<PreAuthSettingsGraph>.navigateToPreAuthFlightRecorder() {
    this.navigate(route = PreAuthSettingsGraph.FlightRecorderRoute)
}

package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.PreAuthSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the appearance screen to the settings navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.appearanceDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<SettingsGraph.AppearanceRoute> {
        AppearanceScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Add the appearance screen to the pre-authentication settings navigation entries.
 */
fun EntryProviderScope<PreAuthSettingsGraph>.preAuthAppearanceDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<PreAuthSettingsGraph.AppearanceRoute> {
        AppearanceScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the appearance screen.
 */
fun NavigationController<SettingsGraph>.navigateToAppearance() {
    this.navigate(route = SettingsGraph.AppearanceRoute)
}

/**
 * Navigate to the pre-authentication appearance screen.
 */
fun NavigationController<PreAuthSettingsGraph>.navigateToPreAuthAppearance() {
    this.navigate(route = PreAuthSettingsGraph.AppearanceRoute)
}

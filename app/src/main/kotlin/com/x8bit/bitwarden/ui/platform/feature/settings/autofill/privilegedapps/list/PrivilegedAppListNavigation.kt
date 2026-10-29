@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the privileged apps list screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.privilegedAppsListDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<SettingsGraph.PrivilegedAppListRoute> {
        PrivilegedAppsListScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the privileged apps list screen.
 */
fun NavigationController<SettingsGraph>.navigateToPrivilegedAppsList() {
    this.navigate(route = SettingsGraph.PrivilegedAppListRoute)
}

@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Helper function to navigate to the import items screen.
 */
fun NavigationController<SettingsGraph>.navigateToImportItemsScreen() {
    this.navigate(route = SettingsGraph.ImportItemsRoute)
}

/**
 * Add the import items screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.importItemsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
) {
    pushEntry<SettingsGraph.ImportItemsRoute> {
        ImportItemsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToImportFromComputer = onNavigateToImportLogins,
        )
    }
}

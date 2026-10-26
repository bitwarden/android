package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph

/**
 * Add the block autofill screen to the navigation entries.
 */
fun EntryProviderScope<SettingsGraph>.blockAutoFillDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<SettingsGraph.BlockAutofillRoute> {
        BlockAutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the block autofill screen.
 */
fun NavigationController<SettingsGraph>.navigateToBlockAutoFillScreen() {
    this.navigate(route = SettingsGraph.BlockAutofillRoute)
}

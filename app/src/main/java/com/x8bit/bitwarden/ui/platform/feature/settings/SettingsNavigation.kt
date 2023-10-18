package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE: String = "settings"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.settingsDestinations(
    onNavigateToAccountSecurity: () -> Unit,
) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(onNavigateToAccountSecurity = onNavigateToAccountSecurity)
    }
}

/**
 * Navigate to the settings screen screen.
 */
fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(SETTINGS_ROUTE, navOptions)
}

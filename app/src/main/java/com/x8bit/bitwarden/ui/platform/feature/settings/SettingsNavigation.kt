package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation

const val SETTINGS_GRAPH_ROUTE: String = "settings_graph"
private const val SETTINGS_ROUTE: String = "settings"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
) {
    navigation(
        startDestination = SETTINGS_ROUTE,
        route = SETTINGS_GRAPH_ROUTE,
    ) {
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateToAccountSecurity = { navController.navigateToAccountSecurity() },
            )
        }
        accountSecurityDestination(onNavigateBack = { navController.popBackStack() })
    }
}

/**
 * Navigate to the settings screen screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(SETTINGS_GRAPH_ROUTE, navOptions)
}

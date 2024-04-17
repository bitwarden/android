package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.platform.base.util.composableWithRootPushTransitions

const val SETTINGS_GRAPH_ROUTE = "settings_graph"
private const val SETTINGS_ROUTE = "settings"

/**
 * Add settings graph to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onNavigateToTutorial: () -> Unit,
) {
    navigation(
        startDestination = SETTINGS_ROUTE,
        route = SETTINGS_GRAPH_ROUTE
    ) {
        composableWithRootPushTransitions(
            route = SETTINGS_ROUTE
        ) {
            SettingsScreen(
                onNavigateToTutorial = onNavigateToTutorial,
            )
        }
    }
}

/**
 * Navigate to the settings screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(SETTINGS_GRAPH_ROUTE, navOptions)
}

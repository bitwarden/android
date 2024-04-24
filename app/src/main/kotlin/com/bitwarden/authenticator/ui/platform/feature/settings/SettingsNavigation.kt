package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.platform.base.util.composableWithRootPushTransitions
import com.bitwarden.authenticator.ui.platform.feature.settings.export.exportDestination
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToSettingsTutorial
import com.bitwarden.authenticator.ui.platform.feature.tutorial.tutorialSettingsDestination

const val SETTINGS_GRAPH_ROUTE = "settings_graph"
private const val SETTINGS_ROUTE = "settings"

/**
 * Add settings graph to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onNavigateToExport: () -> Unit,
) {
    navigation(
        startDestination = SETTINGS_ROUTE,
        route = SETTINGS_GRAPH_ROUTE
    ) {
        composableWithRootPushTransitions(
            route = SETTINGS_ROUTE
        ) {
            SettingsScreen(
                onNavigateToTutorial = { navController.navigateToSettingsTutorial() },
                onNavigateToExport = onNavigateToExport,
            )
        }
        tutorialSettingsDestination(
            onTutorialFinished = { navController.popBackStack() }
        )
        exportDestination(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

/**
 * Navigate to the settings screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(SETTINGS_GRAPH_ROUTE, navOptions)
}

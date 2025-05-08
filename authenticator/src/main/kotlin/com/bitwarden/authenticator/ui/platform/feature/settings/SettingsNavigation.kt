package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.platform.feature.settings.export.exportDestination
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.importingDestination
import com.bitwarden.authenticator.ui.platform.feature.tutorial.tutorialSettingsDestination
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the settings graph.
 */
@Serializable
data object SettingsGraphRoute

/**
 * The type-safe route for the settings screen.
 */
@Serializable
data object SettingsRoute

/**
 * Add settings graph to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTutorial: () -> Unit,
) {
    navigation<SettingsGraphRoute>(
        startDestination = SettingsRoute,
    ) {
        composableWithRootPushTransitions<SettingsRoute> {
            SettingsScreen(
                onNavigateToTutorial = onNavigateToTutorial,
                onNavigateToExport = onNavigateToExport,
                onNavigateToImport = onNavigateToImport,
            )
        }
        tutorialSettingsDestination(
            onTutorialFinished = { navController.popBackStack() },
        )
        exportDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        importingDestination(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

/**
 * Navigate to the settings screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(route = SettingsGraphRoute, navOptions = navOptions)
}

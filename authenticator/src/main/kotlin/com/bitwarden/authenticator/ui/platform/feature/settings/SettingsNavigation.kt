package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.platform.feature.settings.export.exportDestination
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.importingDestination
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
 * Add settings destination to the nav graph.
 */
fun NavGraphBuilder.settingsDestination(
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTutorial: () -> Unit,
) {
    composableWithRootPushTransitions<SettingsRoute> {
        SettingsScreen(
            onNavigateToTutorial = onNavigateToTutorial,
            onNavigateToExport = onNavigateToExport,
            onNavigateToImport = onNavigateToImport,
        )
    }
}

/**
 * Add settings graph to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    onNavigateBack: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
) {
    navigation<SettingsGraphRoute>(
        startDestination = SettingsRoute,
    ) {
        settingsDestination(
            onNavigateToTutorial = onNavigateToTutorial,
            onNavigateToExport = onNavigateToExport,
            onNavigateToImport = onNavigateToImport,
        )
        exportDestination(
            onNavigateBack = onNavigateBack,
        )
        importingDestination(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the settings screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(route = SettingsGraphRoute, navOptions = navOptions)
}

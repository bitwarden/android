package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val ABOUT_ROUTE = "settings_about"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.aboutDestination(
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
) {
    composableWithPushTransitions(
        route = ABOUT_ROUTE,
    ) {
        AboutScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToFlightRecorder = onNavigateToFlightRecorder,
            onNavigateToRecordedLogs = onNavigateToRecordedLogs,
        )
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToAbout(navOptions: NavOptions? = null) {
    navigate(ABOUT_ROUTE, navOptions)
}

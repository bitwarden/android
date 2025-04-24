package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val PRE_AUTH_ABOUT_ROUTE = "pre_auth_settings_about"
private const val ABOUT_ROUTE = "settings_about"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.aboutDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
) {
    composableWithPushTransitions(
        route = getRoute(isPreAuth = isPreAuth),
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
fun NavController.navigateToAbout(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = getRoute(isPreAuth = isPreAuth), navOptions = navOptions)
}

private fun getRoute(
    isPreAuth: Boolean,
): String = if (isPreAuth) PRE_AUTH_ABOUT_ROUTE else ABOUT_ROUTE

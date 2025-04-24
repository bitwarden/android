package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val PRE_AUTH_FLIGHT_RECORDER_ROUTE = "pre_auth_flight_recorder_config"
private const val FLIGHT_RECORDER_ROUTE = "flight_recorder_config"

/**
 * Add flight recorder destination to the nav graph.
 */
fun NavGraphBuilder.flightRecorderDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = getRoute(isPreAuth = isPreAuth),
    ) {
        FlightRecorderScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the flight recorder screen.
 */
fun NavController.navigateToFlightRecorder(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = getRoute(isPreAuth = isPreAuth), navOptions = navOptions)
}

private fun getRoute(
    isPreAuth: Boolean,
): String = if (isPreAuth) PRE_AUTH_FLIGHT_RECORDER_ROUTE else FLIGHT_RECORDER_ROUTE

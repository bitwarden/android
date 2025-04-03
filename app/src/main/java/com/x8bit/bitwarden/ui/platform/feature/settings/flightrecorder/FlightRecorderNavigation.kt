package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val FLIGHT_RECORDER_ROUTE = "flight_recorder_config"

/**
 * Add flight recorder destination to the nav graph.
 */
fun NavGraphBuilder.flightRecorderDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = FLIGHT_RECORDER_ROUTE,
    ) {
        FlightRecorderScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the flight recorder screen.
 */
fun NavController.navigateToFlightRecorder(navOptions: NavOptions? = null) {
    navigate(FLIGHT_RECORDER_ROUTE, navOptions)
}

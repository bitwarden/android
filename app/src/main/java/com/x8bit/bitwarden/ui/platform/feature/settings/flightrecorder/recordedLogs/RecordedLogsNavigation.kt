package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val PRE_AUTH_FLIGHT_RECORDER_RECORDED_LOGS_ROUTE =
    "pre_auth_flight_recorder_recorded_logs"
private const val FLIGHT_RECORDER_RECORDED_LOGS_ROUTE = "flight_recorder_recorded_logs"

/**
 * Add recorded logs destination to the nav graph.
 */
fun NavGraphBuilder.recordedLogsDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = getRoute(isPreAuth = isPreAuth),
    ) {
        RecordedLogsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the flight recorder recorded logs screen.
 */
fun NavController.navigateToRecordedLogs(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = getRoute(isPreAuth = isPreAuth), navOptions = navOptions)
}

private fun getRoute(
    isPreAuth: Boolean,
): String =
    if (isPreAuth) {
        PRE_AUTH_FLIGHT_RECORDER_RECORDED_LOGS_ROUTE
    } else {
        FLIGHT_RECORDER_RECORDED_LOGS_ROUTE
    }

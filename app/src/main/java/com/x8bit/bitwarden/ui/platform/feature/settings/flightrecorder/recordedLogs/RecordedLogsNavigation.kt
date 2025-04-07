package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val FLIGHT_RECORDER_RECORDED_LOGS_ROUTE = "flight_recorder_recorded_logs"

/**
 * Add recorded logs destination to the nav graph.
 */
fun NavGraphBuilder.recordedLogsDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = FLIGHT_RECORDER_RECORDED_LOGS_ROUTE,
    ) {
        RecordedLogsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the flight recorder recorded logs screen.
 */
fun NavController.navigateToRecordedLogs(navOptions: NavOptions? = null) {
    navigate(FLIGHT_RECORDER_RECORDED_LOGS_ROUTE, navOptions)
}

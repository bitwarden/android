package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the recorded logs screen.
 */
@Serializable
sealed class RecordedLogsRoute {
    /**
     * The type-safe route for the recorded logs screen.
     */
    @Serializable
    data object Standard : RecordedLogsRoute()

    /**
     * The type-safe route for the pre-auth recorded logs screen.
     */
    @Serializable
    data object PreAuth : RecordedLogsRoute()
}

/**
 * Add recorded logs destination to the nav graph.
 */
fun NavGraphBuilder.recordedLogsDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPreAuth) {
        composableWithSlideTransitions<RecordedLogsRoute.PreAuth> {
            RecordedLogsScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    } else {
        composableWithSlideTransitions<RecordedLogsRoute.Standard> {
            RecordedLogsScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

/**
 * Navigate to the flight recorder recorded logs screen.
 */
fun NavController.navigateToRecordedLogs(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = if (isPreAuth) RecordedLogsRoute.PreAuth else RecordedLogsRoute.Standard,
        navOptions = navOptions,
    )
}

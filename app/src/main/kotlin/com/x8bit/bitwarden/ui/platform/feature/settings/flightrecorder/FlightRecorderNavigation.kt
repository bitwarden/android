package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the flight recorder screen.
 */
@Serializable
sealed class FlightRecorderRoute {
    /**
     * The type-safe route for the flight recorder screen.
     */
    @Serializable
    data object Standard : FlightRecorderRoute()

    /**
     * The type-safe route for the pre-auth flight recorder screen.
     */
    @Serializable
    data object PreAuth : FlightRecorderRoute()
}

/**
 * Add flight recorder destination to the nav graph.
 */
fun NavGraphBuilder.flightRecorderDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPreAuth) {
        composableWithSlideTransitions<FlightRecorderRoute.PreAuth> {
            FlightRecorderScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    } else {
        composableWithSlideTransitions<FlightRecorderRoute.Standard> {
            FlightRecorderScreen(
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

/**
 * Navigate to the flight recorder screen.
 */
fun NavController.navigateToFlightRecorder(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = if (isPreAuth) FlightRecorderRoute.PreAuth else FlightRecorderRoute.Standard,
        navOptions = navOptions,
    )
}

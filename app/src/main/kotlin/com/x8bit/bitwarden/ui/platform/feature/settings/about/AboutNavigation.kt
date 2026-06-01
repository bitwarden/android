package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the settings about screen.
 */
@Serializable
sealed class SettingsAboutRoute {
    /**
     * The type-safe route for the settings about screen.
     */
    @Serializable
    data object Standard : SettingsAboutRoute()

    /**
     * The type-safe route for the pre-auth settings about screen.
     */
    @Serializable
    data object PreAuth : SettingsAboutRoute()
}

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.aboutDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
) {
    if (isPreAuth) {
        composableWithPushTransitions<SettingsAboutRoute.PreAuth> {
            AboutScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToFlightRecorder = onNavigateToFlightRecorder,
                onNavigateToRecordedLogs = onNavigateToRecordedLogs,
            )
        }
    } else {
        composableWithPushTransitions<SettingsAboutRoute.Standard> {
            AboutScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToFlightRecorder = onNavigateToFlightRecorder,
                onNavigateToRecordedLogs = onNavigateToRecordedLogs,
            )
        }
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToAbout(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = if (isPreAuth) SettingsAboutRoute.PreAuth else SettingsAboutRoute.Standard,
        navOptions = navOptions,
    )
}

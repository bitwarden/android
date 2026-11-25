package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the pre-authentication Settings UI navigation graph and provides
 * type-safe routes to each destination available in the graph.
 */
@Serializable
sealed class PreAuthSettingsGraph : NavKey {
    /**
     * The type-safe route for the pre-authentication settings screen.
     */
    @Serializable
    data object SettingsRoute : PreAuthSettingsGraph()

    /**
     * The type-safe route for the pre-authentication appearance screen.
     */
    @Serializable
    data object AppearanceRoute : PreAuthSettingsGraph()

    /**
     * The type-safe route for the pre-authentication other screen.
     */
    @Serializable
    data object OtherRoute : PreAuthSettingsGraph()

    /**
     * The type-safe route for the pre-authentication about screen.
     */
    @Serializable
    data object AboutRoute : PreAuthSettingsGraph()

    /**
     * The type-safe route for the pre-authentication flight recorder screen.
     */
    @Serializable
    data object FlightRecorderRoute : PreAuthSettingsGraph()

    /**
     * The type-safe route for the pre-authentication recorded logs screen.
     */
    @Serializable
    data object RecordedLogsRoute : PreAuthSettingsGraph()
}

package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Settings tab UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class SettingsGraph : NavKey {
    /**
     * The type-safe route for the settings screen.
     */
    @Serializable
    data object SettingsRoute : SettingsGraph()

    /**
     * The type-safe route for the export screen.
     */
    @Serializable
    data object ExportRoute : SettingsGraph()

    /**
     * The type-safe route for the import screen.
     */
    @Serializable
    data object ImportRoute : SettingsGraph()
}

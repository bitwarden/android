package com.x8bit.bitwarden.ui.platform.feature.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Main UI navigation graph and provides type-safe routes to each
 * destination available in the graph.
 */
@Serializable
sealed class MainGraph : NavKey {
    /**
     * The type-safe route for the overlay navigation screen.
     */
    @Serializable
    data object OverlayNavRoute : MainGraph()

    /**
     * The type-safe route for the debug screen.
     */
    @Serializable
    data object DebugRoute : MainGraph()
}

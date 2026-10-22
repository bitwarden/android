package com.bitwarden.testharness.ui.platform.feature.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Main UI navigation graph and provides type-safe routes to each
 * destination available in the graph.
 */
@Serializable
sealed class MainGraph : NavKey {
    /**
     * The type-safe route for the root navigation screen.
     */
    @Serializable
    data object RootNavigationRoute : MainGraph()
}

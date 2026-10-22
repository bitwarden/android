package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Generator tab UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class GeneratorGraph : NavKey {
    /**
     * The type-safe route for the generator screen.
     */
    @Serializable
    data object GeneratorRoute : GeneratorGraph()
}

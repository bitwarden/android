package com.bitwarden.testharness.ui.platform.feature.autofill

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Autofill test UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class AutofillGraph : NavKey {
    /**
     * The type-safe route for the Autofill test placeholder screen, the root of the Autofill test
     * flow.
     */
    @Serializable
    data object AutofillPlaceholderRoute : AutofillGraph()
}

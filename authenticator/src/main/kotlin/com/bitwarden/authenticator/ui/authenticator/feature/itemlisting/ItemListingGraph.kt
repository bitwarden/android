package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Verification Codes tab UI navigation graph and provides
 * type-safe routes to each destination available in the graph.
 */
@Serializable
sealed class ItemListingGraph : NavKey {
    /**
     * The type-safe route for the item listing screen.
     */
    @Serializable
    data object ItemListingRoute : ItemListingGraph()
}

package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Send tab UI navigation graph and provides type-safe routes to
 * each destination available in the graph.
 */
@Serializable
sealed class SendGraph : NavKey {
    /**
     * The type-safe route for the send screen.
     */
    @Serializable
    data object SendRoute : SendGraph()

    /**
     * The type-safe route for the send item listing screen.
     */
    @Serializable
    data class SendItemListingRoute(
        val vaultItemListingType: VaultItemListingType,
    ) : SendGraph()
}

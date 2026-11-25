package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Migrate To My Items UI navigation graph and provides type-safe
 * routes to each destination available in the graph.
 */
@Serializable
sealed class MigrateToMyItemsGraph : NavKey {
    /**
     * The type-safe route for the migrate to my items screen.
     */
    @Serializable
    data object MigrateToMyItemsRoute : MigrateToMyItemsGraph()

    /**
     * The type-safe route for the leave organization screen.
     *
     * @property organizationId The ID of the organization to leave.
     * @property organizationName The name of the organization to leave.
     */
    @Serializable
    data class LeaveOrganizationRoute(
        val organizationId: String,
        val organizationName: String,
    ) : MigrateToMyItemsGraph()
}

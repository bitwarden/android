package com.x8bit.bitwarden.ui.vault.feature.exportitems

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Export Items UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class ExportItemsGraph : NavKey {
    /**
     * The type-safe route for the select account screen.
     */
    @Serializable
    data object SelectAccountRoute : ExportItemsGraph()

    /**
     * The type-safe route for the verify password screen.
     */
    @Serializable
    data class VerifyPasswordRoute(
        val userId: String,
        val hasOtherAccounts: Boolean,
    ) : ExportItemsGraph()

    /**
     * The type-safe route for the review export screen.
     *
     * This route is used to navigate to the screen where the user can review the items that will
     * be exported from their vault. This screen operates on the currently active user context for
     * determining which items to review for export.
     */
    @Serializable
    data object ReviewExportRoute : ExportItemsGraph()
}

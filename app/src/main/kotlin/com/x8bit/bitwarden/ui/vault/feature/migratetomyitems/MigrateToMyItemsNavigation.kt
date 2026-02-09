@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the migrate to my items screen.
 */
@OmitFromCoverage
@Serializable
data object MigrateToMyItemsRoute

/**
 * Add the migrate to my items screen to the nav graph.
 */
fun NavGraphBuilder.migrateToMyItemsDestination(
    onNavigateToLeaveOrganization: (organizationId: String, organizationName: String) -> Unit,
) {
    composable<MigrateToMyItemsRoute> {
        MigrateToMyItemsScreen(
            onNavigateToLeaveOrganization = onNavigateToLeaveOrganization,
        )
    }
}

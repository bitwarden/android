@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.leaveOrganizationDestination
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.navigateToLeaveOrganization
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the migrate to my items graph.
 */
@OmitFromCoverage
@Serializable
data object MigrateToMyItemsGraphRoute

/**
 * Navigate to the migrate to my items graph.
 */
fun NavController.navigateToMigrateToMyItemsGraph(
    navOptions: NavOptions? = null,
) {
    navigate(route = MigrateToMyItemsGraphRoute, navOptions = navOptions)
}

/**
 * Add the migrate to my items graph to the nav graph.
 */
fun NavGraphBuilder.migrateToMyItemsGraph(
    navController: NavController,
) {
    navigation<MigrateToMyItemsGraphRoute>(
        startDestination = MigrateToMyItemsRoute,
    ) {
        migrateToMyItemsDestination(
            onNavigateToLeaveOrganization = { organizationId, organizationName ->
                navController.navigateToLeaveOrganization(
                    organizationId = organizationId,
                    organizationName = organizationName,
                )
            },
        )
        leaveOrganizationDestination(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

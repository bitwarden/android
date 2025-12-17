@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the migrate to my items screen.
 */
@OmitFromCoverage
@Serializable
data object MigrateToMyItemsRoute

/**
 * Navigate to the migrate to my items screen.
 */
fun NavController.navigateToMigrateToMyItems(
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = MigrateToMyItemsRoute,
        navOptions = navOptions,
    )
}

/**
 * Add the migrate to my items screen to the nav graph.
 */
fun NavGraphBuilder.migrateToMyItemsDestination(
    onNavigateToVault: () -> Unit,
    onNavigateToLeaveOrganization: () -> Unit,
) {
    composableWithSlideTransitions<MigrateToMyItemsRoute> {
        MigrateToMyItemsScreen(
            onNavigateToVault = onNavigateToVault,
            onNavigateToLeaveOrganization = onNavigateToLeaveOrganization,
        )
    }
}

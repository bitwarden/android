@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the migrate to my items screen.
 *
 * @property organizationId The ID of the organization requiring migration.
 * @property organizationName The name of the organization requiring migration.
 */
@OmitFromCoverage
@Serializable
data class MigrateToMyItemsRoute(
    val organizationId: String,
    val organizationName: String,
)

/**
 * Class to retrieve migrate to my items arguments from the [SavedStateHandle].
 *
 * @property organizationId The ID of the organization requiring migration.
 * @property organizationName The name of the organization requiring migration.
 */
data class MigrateToMyItemsArgs(
    val organizationId: String,
    val organizationName: String,
)

/**
 * Constructs a [MigrateToMyItemsArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toMigrateToMyItemsArgs(): MigrateToMyItemsArgs {
    val route = this.toRoute<MigrateToMyItemsRoute>()
    return MigrateToMyItemsArgs(
        organizationId = route.organizationId,
        organizationName = route.organizationName,
    )
}

/**
 * Navigate to the migrate to my items screen.
 *
 * @param organizationId The ID of the organization requiring migration.
 * @param organizationName The name of the organization requiring migration.
 */
fun NavController.navigateToMigrateToMyItems(
    organizationId: String,
    organizationName: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = MigrateToMyItemsRoute(
            organizationId = organizationId,
            organizationName = organizationName,
        ),
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

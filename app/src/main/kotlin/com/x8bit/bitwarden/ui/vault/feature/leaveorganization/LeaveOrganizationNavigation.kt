@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the leave organization screen.
 *
 * @property organizationId The ID of the organization to leave.
 * @property organizationName The name of the organization to leave.
 */
@OmitFromCoverage
@Serializable
data class LeaveOrganizationRoute(
    val organizationId: String,
    val organizationName: String,
)

/**
 * Class to retrieve leave organization arguments from the [SavedStateHandle].
 *
 * @property organizationId The ID of the organization to leave.
 * @property organizationName The name of the organization to leave.
 */
data class LeaveOrganizationArgs(
    val organizationId: String,
    val organizationName: String,
)

/**
 * Constructs a [LeaveOrganizationArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toLeaveOrganizationArgs(): LeaveOrganizationArgs {
    val route = this.toRoute<LeaveOrganizationRoute>()
    return LeaveOrganizationArgs(
        organizationId = route.organizationId,
        organizationName = route.organizationName,
    )
}

/**
 * Add the leave organization screen to the nav graph.
 */
fun NavGraphBuilder.leaveOrganizationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
) {
    composableWithPushTransitions<LeaveOrganizationRoute> {
        LeaveOrganizationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVault = onNavigateToVault,
        )
    }
}

/**
 * Navigate to the leave organization screen.
 *
 * @param organizationId The ID of the organization to leave.
 * @param organizationName The name of the organization to leave.
 */
fun NavController.navigateToLeaveOrganization(
    organizationId: String,
    organizationName: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = LeaveOrganizationRoute(
            organizationId = organizationId,
            organizationName = organizationName,
        ),
        navOptions = navOptions,
    )
}

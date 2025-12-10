@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.leaveorganization

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the leave organization screen.
 */
@OmitFromCoverage
@Serializable
data object LeaveOrganizationRoute

/**
 * Add the leave organization screen to the nav graph.
 */
fun NavGraphBuilder.leaveOrganizationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
) {
    composableWithSlideTransitions<LeaveOrganizationRoute> {
        LeaveOrganizationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVault = onNavigateToVault,
        )
    }
}

/**
 * Navigate to the leave organization screen.
 */
fun NavController.navigateToLeaveOrganization(
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = LeaveOrganizationRoute,
        navOptions = navOptions,
    )
}

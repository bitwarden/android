package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the pending requests screen.
 */
@Serializable
data object PendingRequestsRoute

/**
 * Add pending requests destinations to the nav graph.
 */
fun NavGraphBuilder.pendingRequestsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprintPhrase: String) -> Unit,
) {
    composableWithSlideTransitions<PendingRequestsRoute> {
        PendingRequestsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLoginApproval = onNavigateToLoginApproval,
        )
    }
}

/**
 * Navigate to the Pending Login Requests screen.
 */
fun NavController.navigateToPendingRequests(navOptions: NavOptions? = null) {
    this.navigate(route = PendingRequestsRoute, navOptions = navOptions)
}

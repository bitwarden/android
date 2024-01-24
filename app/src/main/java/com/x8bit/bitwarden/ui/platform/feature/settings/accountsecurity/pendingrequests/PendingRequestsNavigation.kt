package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val PENDING_REQUESTS_ROUTE = "pending_requests"

/**
 * Add pending requests destinations to the nav graph.
 */
fun NavGraphBuilder.pendingRequestsDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = PENDING_REQUESTS_ROUTE,
    ) {
        PendingRequestsScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the Pending Login Requests screen.
 */
fun NavController.navigateToPendingRequests(navOptions: NavOptions? = null) {
    navigate(PENDING_REQUESTS_ROUTE, navOptions)
}

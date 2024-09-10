package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val EXPIRED_REGISTRATION_LINK_ROUTE = "expired_registration_link"

/**
 * Navigate to the expired registration link screen.
 */
fun NavController.navigateToExpiredRegistrationLinkScreen(navOptions: NavOptions? = null) {
    this.navigate(route = EXPIRED_REGISTRATION_LINK_ROUTE, navOptions = navOptions)
}

/**
 * Add the expired registration link screen to the nav graph.
 */
fun NavGraphBuilder.expiredRegistrationLinkDestination(
    onNavigateBack: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    composableWithPushTransitions(
        route = EXPIRED_REGISTRATION_LINK_ROUTE,
    ) {
        ExpiredRegistrationLinkScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToStartRegistration = onNavigateToStartRegistration,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

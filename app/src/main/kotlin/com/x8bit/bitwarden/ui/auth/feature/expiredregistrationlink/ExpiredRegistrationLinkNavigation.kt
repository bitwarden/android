package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the expired registration link screen.
 */
@Serializable
data object ExpiredRegistrationLinkRoute

/**
 * Navigate to the expired registration link screen.
 */
fun NavController.navigateToExpiredRegistrationLinkScreen(navOptions: NavOptions? = null) {
    this.navigate(route = ExpiredRegistrationLinkRoute, navOptions = navOptions)
}

/**
 * Add the expired registration link screen to the nav graph.
 */
fun NavGraphBuilder.expiredRegistrationLinkDestination(
    onNavigateBack: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    composableWithPushTransitions<ExpiredRegistrationLinkRoute> {
        ExpiredRegistrationLinkScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToStartRegistration = onNavigateToStartRegistration,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

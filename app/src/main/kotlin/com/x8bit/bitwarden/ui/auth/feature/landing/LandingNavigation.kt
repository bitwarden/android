package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithStayTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the landing screen.
 */
@Serializable
data object LandingRoute

/**
 * Navigate to the landing screen.
 */
fun NavController.navigateToLanding(navOptions: NavOptions? = null) {
    this.navigate(route = LandingRoute, navOptions = navOptions)
}

/**
 * Add the Landing screen to the nav graph.
 */
fun NavGraphBuilder.landingDestination(
    onNavigateToLogin: (emailAddress: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    onNavigateToPreAuthSettings: () -> Unit,
) {
    composableWithStayTransitions<LandingRoute> {
        LandingScreen(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToEnvironment = onNavigateToEnvironment,
            onNavigateToStartRegistration = onNavigateToStartRegistration,
            onNavigateToPreAuthSettings = onNavigateToPreAuthSettings,
        )
    }
}

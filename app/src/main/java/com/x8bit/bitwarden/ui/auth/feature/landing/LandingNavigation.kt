package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

const val LANDING_ROUTE: String = "landing"

/**
 * Navigate to the landing screen.
 */
fun NavController.navigateToLanding(navOptions: NavOptions? = null) {
    this.navigate(LANDING_ROUTE, navOptions)
}

/**
 * Add the Landing screen to the nav graph.
 */
fun NavGraphBuilder.landingDestinations(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: (emailAddress: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
) {
    composable(
        route = LANDING_ROUTE,
        enterTransition = TransitionProviders.Enter.stay,
        exitTransition = TransitionProviders.Exit.stay,
        popEnterTransition = TransitionProviders.Enter.stay,
        popExitTransition = TransitionProviders.Exit.stay,
    ) {
        LandingScreen(
            onNavigateToCreateAccount = onNavigateToCreateAccount,
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToEnvironment = onNavigateToEnvironment,
        )
    }
}

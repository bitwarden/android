package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

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
fun NavGraphBuilder.landingDestination(onNavigateToCreateAccount: () -> Unit) {
    composable(route = LANDING_ROUTE) {
        LandingScreen(onNavigateToCreateAccount = onNavigateToCreateAccount)
    }
}

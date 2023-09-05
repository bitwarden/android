package com.x8bit.bitwarden.ui.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.feature.createaccount.createAccountDestinations
import com.x8bit.bitwarden.ui.feature.createaccount.navigateToCreateAccount
import com.x8bit.bitwarden.ui.feature.landing.LANDING_ROUTE
import com.x8bit.bitwarden.ui.feature.landing.landingDestination

const val AUTH_ROUTE: String = "auth"

/**
 * Add auth destinations to the nav graph.
 */
fun NavGraphBuilder.authDestinations(navController: NavHostController) {
    navigation(
        startDestination = LANDING_ROUTE,
        route = AUTH_ROUTE,
    ) {
        createAccountDestinations()
        landingDestination(
            onNavigateToCreateAccount = { navController.navigateToCreateAccount() },
        )
    }
}

/**
 * Navigate to the auth screen. Note this will only work if auth destination was added
 * via [authDestinations].
 */
fun NavController.navigateToAuth(
    navOptions: NavOptions? = null,
) {
    navigate(LANDING_ROUTE, navOptions)
}

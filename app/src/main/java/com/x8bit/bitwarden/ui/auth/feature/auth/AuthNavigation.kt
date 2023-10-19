package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.createaccount.createAccountDestinations
import com.x8bit.bitwarden.ui.auth.feature.createaccount.navigateToCreateAccount
import com.x8bit.bitwarden.ui.auth.feature.landing.LANDING_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.landing.landingDestinations
import com.x8bit.bitwarden.ui.auth.feature.login.loginDestinations
import com.x8bit.bitwarden.ui.auth.feature.login.navigateToLogin

const val AUTH_GRAPH_ROUTE: String = "auth_graph"

/**
 * Add auth destinations to the nav graph.
 */
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(
        startDestination = LANDING_ROUTE,
        route = AUTH_GRAPH_ROUTE,
    ) {
        createAccountDestinations(onNavigateBack = { navController.popBackStack() })
        landingDestinations(
            onNavigateToCreateAccount = { navController.navigateToCreateAccount() },
            onNavigateToLogin = { emailAddress, regionLabel ->
                navController.navigateToLogin(emailAddress, regionLabel)
            },
        )
        loginDestinations(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

/**
 * Navigate to the auth screen. Note this will only work if auth destination was added
 * via [authGraph].
 */
fun NavController.navigateToAuthGraph(
    navOptions: NavOptions? = null,
) {
    navigate(AUTH_GRAPH_ROUTE, navOptions)
}

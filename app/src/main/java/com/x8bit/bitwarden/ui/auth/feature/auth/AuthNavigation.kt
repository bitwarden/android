package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.createaccount.createAccountDestinations
import com.x8bit.bitwarden.ui.auth.feature.createaccount.navigateToCreateAccount
import com.x8bit.bitwarden.ui.auth.feature.landing.LANDING_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.landing.landingDestinations
import com.x8bit.bitwarden.ui.auth.feature.login.loginDestinations
import com.x8bit.bitwarden.ui.auth.feature.login.navigateToLogin
import com.x8bit.bitwarden.ui.auth.feature.environment.navigateToEnvironment
import com.x8bit.bitwarden.ui.auth.feature.environment.environmentDestination

const val AUTH_GRAPH_ROUTE: String = "auth_graph"

/**
 * Add auth destinations to the nav graph.
 */
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(
        startDestination = LANDING_ROUTE,
        route = AUTH_GRAPH_ROUTE,
    ) {
        createAccountDestinations(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLogin = { emailAddress, captchaToken ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                    captchaToken = captchaToken,
                    navOptions = navOptions {
                        popUpTo(LANDING_ROUTE)
                    },
                )
            },
        )
        landingDestinations(
            onNavigateToCreateAccount = { navController.navigateToCreateAccount() },
            onNavigateToLogin = { emailAddress ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                    captchaToken = null,
                )
            },
            onNavigateToEnvironment = {
                navController.navigateToEnvironment()
            },
        )
        loginDestinations(
            onNavigateBack = { navController.popBackStack() },
        )
        environmentDestination(
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

package com.x8bit.bitwarden.ui.feature.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.feature.createaccount.createAccountDestinations
import com.x8bit.bitwarden.ui.feature.createaccount.navigateToCreateAccount
import com.x8bit.bitwarden.ui.feature.landing.LANDING_ROUTE
import com.x8bit.bitwarden.ui.feature.landing.landingDestination

const val LOGIN_ROUTE: String = "login"

/**
 * Add login destinations to the nav graph.
 */
fun NavGraphBuilder.loginDestinations(navController: NavHostController) {
    navigation(
        startDestination = LANDING_ROUTE,
        route = LOGIN_ROUTE,
    ) {
        createAccountDestinations()
        landingDestination(
            onNavigateToCreateAccount = { navController.navigateToCreateAccount() },
        )
    }
}

/**
 * Navigate to the login screen. Note this will only work if login destination was added
 * via [loginDestinations].
 */
fun NavController.navigateToLogin(
    navOptions: NavOptions? = null,
) {
    navigate(LANDING_ROUTE, navOptions)
}

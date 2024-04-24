package com.bitwarden.authenticator.ui.platform.feature.splash

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val SPLASH_ROUTE: String = "splash"

/**
 * Add splash destinations to the nav graph.
 */
fun NavGraphBuilder.splashDestination(
    onSplashScreenDismissed: () -> Unit,
    onExitApplication: () -> Unit,
) {
    composable(SPLASH_ROUTE) {
        SplashScreen(
            onNavigateToAuthenticator = onSplashScreenDismissed,
            onExitApplication = onExitApplication,
        )
    }
}

/**
 * Navigate to the splash screen.
 */
fun NavController.navigateToSplash(
    navOptions: NavOptions? = null,
) {
    navigate(SPLASH_ROUTE, navOptions)
}

package com.bitwarden.authenticator.ui.platform.feature.splash

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the splash screen.
 */
@Serializable
data object SplashRoute

/**
 * Add splash destinations to the nav graph.
 */
fun NavGraphBuilder.splashDestination() {
    composable<SplashRoute> { SplashScreen() }
}

/**
 * Navigate to the splash screen.
 */
fun NavController.navigateToSplash(
    navOptions: NavOptions? = null,
) {
    navigate(route = SplashRoute, navOptions = navOptions)
}

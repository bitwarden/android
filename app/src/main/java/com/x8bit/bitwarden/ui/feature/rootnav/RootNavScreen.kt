package com.x8bit.bitwarden.ui.feature.rootnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.components.PlaceholderComposable
import com.x8bit.bitwarden.ui.feature.login.loginDestinations
import com.x8bit.bitwarden.ui.feature.login.navigateToLogin

/**
 * Controls root level [NavHost] for the app.
 */
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        splashDestinations()
        loginDestinations(navController)
    }

    // When state changes, navigate to different root navigation state
    val rootNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(navController.graph.id) {
            inclusive = true
        }
    }
    when (state) {
        RootNavState.Login -> navController.navigateToLogin(rootNavOptions)
        RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
    }
}

/**
 * The functions below should be moved to their respective feature packages once they exist.
 *
 * For an example of how to setup these nav extensions, see NIA project.
 */

/**
 * TODO: move to splash package (BIT-147)
 */
@Suppress("TopLevelPropertyNaming")
private const val SplashRoute = "splash"

/**
 * Add splash destinations to the nav graph.
 *
 * TODO: move to splash package (BIT-147)
 */
private fun NavGraphBuilder.splashDestinations() {
    composable(SplashRoute) {
        PlaceholderComposable(text = "Splash")
    }
}

/**
 * Navigate to the splash screen. Note this will only work if splash destination was added
 * via [splashDestinations].
 *
 * TODO: move to splash package (BIT-147)
 *
 */
private fun NavController.navigateToSplash(
    navOptions: NavOptions? = null,
) {
    navigate(SplashRoute, navOptions)
}

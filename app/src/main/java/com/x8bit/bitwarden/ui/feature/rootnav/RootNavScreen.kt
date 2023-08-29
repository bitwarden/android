package com.x8bit.bitwarden.ui.feature.rootnav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.x8bit.bitwarden.ui.components.PlaceholderComposable
import com.x8bit.bitwarden.ui.feature.createaccount.CreateAccountScreen

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
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        splashDestinations()
        loginDestinations()
    }

    // When state changes, navigate to different root navigation state
    when (state) {
        RootNavState.Login -> navController.navigateToLoginAsRoot()
        RootNavState.Splash -> navController.navigateToSplashAsRoot()
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
private fun NavController.navigateToSplashAsRoot() {
    navigate(SplashRoute) {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(graph.id) {
            inclusive = true
        }
    }
}

/**
 * TODO move to login package(BIT-146)
 */
@Suppress("TopLevelPropertyNaming")
private const val LoginRoute = "login"

/**
 * Add login destinations to the nav graph.
 *
 * TODO: move to login package (BIT-146)
 */
private fun NavGraphBuilder.loginDestinations() {
    composable(LoginRoute) {
        CreateAccountScreen()
    }
}

/**
 * Navigate to the splash screen. Note this will only work if login destination was added
 * via [loginDestinations].
 *
 * TODO: move to login package (BIT-146)
 */
private fun NavController.navigateToLoginAsRoot() {
    navigate(LoginRoute) {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(graph.id) {
            inclusive = true
        }
    }
}

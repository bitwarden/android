package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.auth.feature.auth.authDestinations
import com.x8bit.bitwarden.ui.auth.feature.auth.navigateToAuth
import com.x8bit.bitwarden.ui.platform.feature.splash.SPLASH_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.splash.navigateToSplash
import com.x8bit.bitwarden.ui.platform.feature.splash.splashDestinations
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.navigateToVaultUnlocked
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedDestinations

/**
 * Controls root level [NavHost] for the app.
 */
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val isNotSplashScreen = state != RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) onSplashScreenRemoved()
    }

    NavHost(
        navController = navController,
        startDestination = SPLASH_ROUTE,
    ) {
        splashDestinations()
        authDestinations(navController)
        vaultUnlockedDestinations()
    }

    // When state changes, navigate to different root navigation state
    val rootNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(navController.graph.id) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    when (state) {
        RootNavState.Auth -> navController.navigateToAuth(rootNavOptions)
        RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
        RootNavState.VaultUnlocked -> navController.navigateToVaultUnlocked(rootNavOptions)
    }
}

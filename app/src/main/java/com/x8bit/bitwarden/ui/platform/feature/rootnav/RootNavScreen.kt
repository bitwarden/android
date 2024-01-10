package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.auth.feature.auth.AUTH_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.auth.authGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.navigateToAuthGraph
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.VAULT_UNLOCK_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.navigateToVaultUnlock
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.vaultUnlockDestination
import com.x8bit.bitwarden.ui.platform.feature.splash.SPLASH_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.splash.navigateToSplash
import com.x8bit.bitwarden.ui.platform.feature.splash.splashDestination
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VAULT_UNLOCKED_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.navigateToVaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedGraph
import com.x8bit.bitwarden.ui.platform.theme.RootTransitionProviders
import java.util.concurrent.atomic.AtomicReference

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
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state != RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) onSplashScreenRemoved()
    }

    NavHost(
        navController = navController,
        startDestination = SPLASH_ROUTE,
        enterTransition = RootTransitionProviders.Enter.fadeIn,
        exitTransition = RootTransitionProviders.Exit.fadeOut,
        popEnterTransition = RootTransitionProviders.Enter.fadeIn,
        popExitTransition = RootTransitionProviders.Exit.fadeOut,
    ) {
        splashDestination()
        authGraph(navController)
        vaultUnlockDestination()
        vaultUnlockedGraph(navController)
    }

    val targetRoute = when (state) {
        RootNavState.Auth -> AUTH_GRAPH_ROUTE
        RootNavState.Splash -> SPLASH_ROUTE
        RootNavState.VaultLocked -> VAULT_UNLOCK_ROUTE
        is RootNavState.VaultUnlocked -> VAULT_UNLOCKED_GRAPH_ROUTE
    }
    val currentRoute = navController.currentDestination?.rootLevelRoute()

    // Don't navigate if we are already at the correct root. This notably happens during process
    // death. In this case, the NavHost already restores state, so we don't have to navigate.
    // However, if the route is correct but the underlying state is different, we should still
    // proceed in order to get a fresh version of that route.
    if (currentRoute == targetRoute && previousStateReference.get() == state) {
        previousStateReference.set(state)
        return
    }
    previousStateReference.set(state)

    // When state changes, navigate to different root navigation state
    val rootNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(navController.graph.id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

    when (state) {
        RootNavState.Auth -> navController.navigateToAuthGraph(rootNavOptions)
        RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
        RootNavState.VaultLocked -> navController.navigateToVaultUnlock(rootNavOptions)
        is RootNavState.VaultUnlocked -> navController.navigateToVaultUnlockedGraph(rootNavOptions)
    }
}

/**
 * Helper method that returns the highest level route for the given [NavDestination].
 *
 * As noted above, this can be removed after upgrading to latest compose navigation, since
 * the nav args can prevent us from having to do this check.
 */
@Suppress("ReturnCount")
private fun NavDestination?.rootLevelRoute(): String? {
    if (this == null) {
        return null
    }
    if (parent?.route == null) {
        return route
    }
    return parent.rootLevelRoute()
}

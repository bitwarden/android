package com.x8bit.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ITEM_LISTING_GRAPH_ROUTE
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.navigateToItemListGraph
import com.x8bit.bitwarden.authenticator.ui.platform.feature.splash.SPLASH_ROUTE
import com.x8bit.bitwarden.authenticator.ui.platform.feature.splash.navigateToSplash
import com.x8bit.bitwarden.authenticator.ui.platform.feature.splash.splashDestination
import com.x8bit.bitwarden.authenticator.ui.platform.theme.NonNullEnterTransitionProvider
import com.x8bit.bitwarden.authenticator.ui.platform.theme.NonNullExitTransitionProvider
import com.x8bit.bitwarden.authenticator.ui.platform.theme.RootTransitionProviders
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference

@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsState()
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state !is RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) {
            onSplashScreenRemoved()
        }
    }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow
            .onEach {
                viewModel.trySendAction(RootNavAction.BackStackUpdate)
            }
            .launchIn(this)
    }

    NavHost(
        navController = navController,
        startDestination = SPLASH_ROUTE,
        enterTransition = { toEnterTransition()(this) },
        exitTransition = { toExitTransition()(this) },
        popEnterTransition = { toEnterTransition()(this) },
        popExitTransition = { toExitTransition()(this) },
    ) {
        splashDestination()

        itemListingGraph(navController = navController)
    }

    val targetRoute = when (state) {
        RootNavState.ItemListing -> ITEM_LISTING_GRAPH_ROUTE
        RootNavState.Splash -> SPLASH_ROUTE
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

    LaunchedEffect(state) {
        when (state) {
            RootNavState.ItemListing -> navController.navigateToItemListGraph(rootNavOptions)
            RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
        }
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

/**
 * Define the enter transition for each route.
 */
@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toEnterTransition(): NonNullEnterTransitionProvider =
    when (targetState.destination.rootLevelRoute()) {
        else -> when (initialState.destination.rootLevelRoute()) {
            // Disable transitions when coming from the splash screen
            SPLASH_ROUTE -> RootTransitionProviders.Enter.none
            else -> RootTransitionProviders.Enter.fadeIn
        }
    }

/**
 * Define the exit transition for each route.
 */
@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toExitTransition(): NonNullExitTransitionProvider =
    when (initialState.destination.rootLevelRoute()) {
        // Disable transitions when coming from the splash screen
        SPLASH_ROUTE -> RootTransitionProviders.Exit.none
        else -> when (targetState.destination.rootLevelRoute()) {
            else -> RootTransitionProviders.Exit.fadeOut
        }
    }

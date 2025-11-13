package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.bitwarden.authenticator.ui.auth.unlock.UnlockRoute
import com.bitwarden.authenticator.ui.auth.unlock.navigateToUnlock
import com.bitwarden.authenticator.ui.auth.unlock.unlockDestination
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.AuthenticatorGraphRoute
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.authenticatorGraph
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.navigateToAuthenticatorGraph
import com.bitwarden.authenticator.ui.platform.components.biometrics.BiometricChanges
import com.bitwarden.authenticator.ui.platform.composition.LocalBiometricsManager
import com.bitwarden.authenticator.ui.platform.feature.splash.SplashRoute
import com.bitwarden.authenticator.ui.platform.feature.splash.navigateToSplash
import com.bitwarden.authenticator.ui.platform.feature.splash.splashDestination
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialRoute
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToTutorial
import com.bitwarden.authenticator.ui.platform.feature.tutorial.tutorialDestination
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.theme.NonNullEnterTransitionProvider
import com.bitwarden.ui.platform.theme.NonNullExitTransitionProvider
import com.bitwarden.ui.platform.theme.RootTransitionProviders
import com.bitwarden.ui.platform.util.toObjectNavigationRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference

/**
 * Controls the root level [NavHost] for the app.
 */
@Suppress("LongMethod")
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsState()
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state.navState !is RootNavState.NavState.Splash
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

    BiometricChanges(
        biometricsManager = biometricsManager,
        onBiometricSupportChange = remember(viewModel) {
            {
                viewModel.trySendAction(RootNavAction.BiometricSupportChanged(it))
            }
        },
    )

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        enterTransition = { toEnterTransition()(this) },
        exitTransition = { toExitTransition()(this) },
        popEnterTransition = { toEnterTransition()(this) },
        popExitTransition = { toExitTransition()(this) },
    ) {
        splashDestination()
        tutorialDestination(
            onTutorialFinished = {
                viewModel.trySendAction(RootNavAction.Internal.TutorialFinished)
            },
        )
        unlockDestination(
            onUnlocked = {
                viewModel.trySendAction(RootNavAction.Internal.AppUnlocked)
            },
        )
        authenticatorGraph(navController = navController)
    }

    val targetRoute = when (state.navState) {
        RootNavState.NavState.Splash -> SplashRoute
        RootNavState.NavState.Locked -> UnlockRoute
        RootNavState.NavState.Tutorial -> TutorialRoute
        RootNavState.NavState.Unlocked -> AuthenticatorGraphRoute
    }

    val currentRoute = navController.currentDestination?.rootLevelRoute()
    // Don't navigate if we are already at the correct root. This notably happens during process
    // death. In this case, the NavHost already restores state, so we don't have to navigate.
    // However, if the route is correct but the underlying state is different, we should still
    // proceed in order to get a fresh version of that route.
    if (currentRoute == targetRoute.toObjectNavigationRoute() &&
        previousStateReference.get() == state
    ) {
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
        when (state.navState) {
            RootNavState.NavState.Splash -> {
                navController.navigateToSplash(rootNavOptions)
            }

            RootNavState.NavState.Tutorial -> {
                navController.navigateToTutorial(rootNavOptions)
            }

            RootNavState.NavState.Locked -> {
                navController.navigateToUnlock(rootNavOptions)
            }

            RootNavState.NavState.Unlocked -> {
                navController.navigateToAuthenticatorGraph(rootNavOptions)
            }
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
            SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Enter.none
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
        SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Exit.none
        else -> when (targetState.destination.rootLevelRoute()) {
            else -> RootTransitionProviders.Exit.fadeOut
        }
    }

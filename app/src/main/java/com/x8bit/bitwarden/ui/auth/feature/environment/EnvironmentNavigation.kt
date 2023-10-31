package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val ENVIRONMENT_ROUTE = "environment"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.environmentDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = ENVIRONMENT_ROUTE,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        EnvironmentScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToEnvironment(navOptions: NavOptions? = null) {
    navigate(ENVIRONMENT_ROUTE, navOptions)
}

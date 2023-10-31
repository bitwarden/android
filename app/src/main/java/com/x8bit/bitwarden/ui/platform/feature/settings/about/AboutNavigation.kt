package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val ABOUT_ROUTE = "settings_about"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.aboutDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = ABOUT_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        AboutScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToAbout(navOptions: NavOptions? = null) {
    navigate(ABOUT_ROUTE, navOptions)
}

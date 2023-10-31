package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val APPEARANCE_ROUTE = "settings_appearance"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.appearanceDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = APPEARANCE_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        AppearanceScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the appearance screen.
 */
fun NavController.navigateToAppearance(navOptions: NavOptions? = null) {
    navigate(APPEARANCE_ROUTE, navOptions)
}

package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val OTHER_ROUTE = "settings_other"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.otherDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = OTHER_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        OtherScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToOther(navOptions: NavOptions? = null) {
    navigate(OTHER_ROUTE, navOptions)
}

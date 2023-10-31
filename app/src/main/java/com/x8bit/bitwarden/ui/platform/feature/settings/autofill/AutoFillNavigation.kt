package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val AUTO_FILL_ROUTE = "settings_auto_fill"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.autoFillDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = AUTO_FILL_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        AutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the auto-fill screen.
 */
fun NavController.navigateToAutoFill(navOptions: NavOptions? = null) {
    navigate(AUTO_FILL_ROUTE, navOptions)
}

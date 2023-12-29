package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val ENVIRONMENT_ROUTE = "environment"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.environmentDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ENVIRONMENT_ROUTE,
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

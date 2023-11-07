package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val NEW_SEND_ROUTE = "new_send"

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.newSendDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = NEW_SEND_ROUTE,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.slideDown,
        popEnterTransition = TransitionProviders.Enter.slideUp,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        NewSendScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToNewSend(navOptions: NavOptions? = null) {
    navigate(NEW_SEND_ROUTE, navOptions)
}

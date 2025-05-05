package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.authenticator.ui.platform.base.util.composableWithPushTransitions

private const val DEBUG_MENU = "debug_menu"

/**
 * Navigate to the setup unlock screen.
 */
fun NavController.navigateToDebugMenuScreen() {
    this.navigate(DEBUG_MENU) {
        launchSingleTop = true
    }
}

/**
 * Add the setup unlock screen to the nav graph.
 */
fun NavGraphBuilder.setupDebugMenuDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = DEBUG_MENU,
    ) {
        DebugMenuScreen(onNavigateBack = onNavigateBack)
    }
}

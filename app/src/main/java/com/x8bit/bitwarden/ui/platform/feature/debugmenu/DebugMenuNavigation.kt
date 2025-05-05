@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

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
fun NavGraphBuilder.debugMenuDestination(
    onNavigateBack: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    composableWithPushTransitions(
        route = DEBUG_MENU,
    ) {
        DebugMenuScreen(onNavigateBack = onNavigateBack)
        // If we are displaying the debug screen, then we can just hide the splash screen.
        onSplashScreenRemoved()
    }
}

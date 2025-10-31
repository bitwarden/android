package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the debug screen.
 */
@Serializable
data object DebugRoute

/**
 * Navigate to the setup unlock screen.
 */
fun NavController.navigateToDebugMenuScreen() {
    this.navigate(route = DebugRoute) {
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
    composableWithPushTransitions<DebugRoute> {
        DebugMenuScreen(onNavigateBack = onNavigateBack)
        // If we are displaying the debug screen, then we can just hide the splash screen.
        onSplashScreenRemoved()
    }
}

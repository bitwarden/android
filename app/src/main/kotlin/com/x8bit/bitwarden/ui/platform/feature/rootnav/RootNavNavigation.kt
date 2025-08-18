package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * The route for the root navigation screen.
 */
const val ROOT_ROUTE: String = "root"

/**
 * Add the root navigation screen to the nav graph.
 */
fun NavGraphBuilder.rootNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    composable(route = ROOT_ROUTE) {
        RootNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

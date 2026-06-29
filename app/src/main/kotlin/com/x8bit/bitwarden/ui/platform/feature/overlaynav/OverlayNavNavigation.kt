package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the overlay navigation screen.
 */
@Serializable
data object OverlayNavRoute

/**
 * Add the overlay navigation screen to the nav graph.
 */
fun NavGraphBuilder.overlayNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    composable<OverlayNavRoute> {
        OverlayNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

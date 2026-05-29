@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the local network access screen.
 */
@OmitFromCoverage
@Serializable
data object LocalNetworkAccessRoute

/**
 * Add the local network access screen to the nav graph.
 */
fun NavGraphBuilder.localNetworkAccessDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    composableWithSlideTransitions<LocalNetworkAccessRoute> {
        LocalNetworkAccessScreen(onDismiss = onDismiss)
        // If we are displaying the local network access screen, then we can just hide
        // the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the local network access screen.
 */
fun NavController.navigateToLocalNetworkAccess() {
    this.navigate(route = LocalNetworkAccessRoute) {
        launchSingleTop = true
    }
}

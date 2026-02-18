@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the cookie acquisition screen.
 */
@OmitFromCoverage
@Serializable
data object CookieAcquisitionRoute

/**
 * Add the cookie acquisition screen to the nav graph.
 */
fun NavGraphBuilder.cookieAcquisitionDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    composableWithSlideTransitions<CookieAcquisitionRoute> {
        CookieAcquisitionScreen(onDismiss = onDismiss)
        // If we are displaying the debug screen, then we can just hide the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the cookie acquisition screen.
 */
fun NavController.navigateToCookieAcquisition() {
    this.navigate(route = CookieAcquisitionRoute) {
        launchSingleTop = true
    }
}

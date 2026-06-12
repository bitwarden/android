@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the accessibility disclosure screen.
 */
@OmitFromCoverage
@Serializable
data object AccessibilityDisclosureRoute

/**
 * Add the accessibility disclosure screen to the nav graph.
 */
fun NavGraphBuilder.accessibilityDisclosureDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    composableWithSlideTransitions<AccessibilityDisclosureRoute> {
        AccessibilityDisclosureScreen(onDismiss = onDismiss)
        // If we are displaying the accessibility disclosure screen, then we can just hide
        // the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the accessibility disclosure screen.
 */
fun NavController.navigateToAccessibilityDisclosure() {
    this.navigate(route = AccessibilityDisclosureRoute) {
        launchSingleTop = true
    }
}

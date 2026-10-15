@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.overlaynav.OverlayNavGraph

/**
 * Add the accessibility disclosure screen to the navigation entries.
 */
fun EntryProviderScope<OverlayNavGraph>.accessibilityDisclosureDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    slideUpEntry<OverlayNavGraph.AccessibilityDisclosureRoute> {
        AccessibilityDisclosureScreen(onDismiss = onDismiss)
        // If we are displaying the accessibility disclosure screen, then we can just hide
        // the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the accessibility disclosure screen.
 */
fun NavigationController<OverlayNavGraph>.navigateToAccessibilityDisclosure() {
    this.addSingleTop(route = OverlayNavGraph.AccessibilityDisclosureRoute)
}

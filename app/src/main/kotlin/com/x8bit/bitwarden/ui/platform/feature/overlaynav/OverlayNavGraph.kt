package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Overlay UI navigation graph and provides type-safe routes to
 * each destination available in the graph.
 */
@Serializable
sealed class OverlayNavGraph : NavKey {
    /**
     * The type-safe route for the root navigation screen.
     */
    @Serializable
    data object RootNavigationRoute : OverlayNavGraph()

    /**
     * The type-safe route for the cookie acquisition screen.
     */
    @Serializable
    data object CookieAcquisitionRoute : OverlayNavGraph()

    /**
     * The type-safe route for the local network access screen.
     */
    @Serializable
    data object LocalNetworkAccessRoute : OverlayNavGraph()

    /**
     * The type-safe route for the accessibility disclosure screen.
     */
    @Serializable
    data object AccessibilityDisclosureRoute : OverlayNavGraph()

    /**
     * The type-safe route for the Update Biometrics screen.
     */
    @Serializable
    data object UpdateBiometricsRoute : OverlayNavGraph()
}

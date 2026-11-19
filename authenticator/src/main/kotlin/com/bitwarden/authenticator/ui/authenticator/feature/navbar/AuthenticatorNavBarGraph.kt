package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.navigation3.runtime.NavKey
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingGraph
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Authenticator Navigation Bar UI navigation graph and provides
 * type-safe routes to each tab available in the graph. Each tab hosts its own nested navigation
 * graph.
 */
@Serializable
sealed class AuthenticatorNavBarGraph : NavKey {
    /**
     * The type-safe route for the verification codes tab, which hosts the [ItemListingGraph].
     */
    @Serializable
    data object VerificationCodes : AuthenticatorNavBarGraph()

    /**
     * The type-safe route for the settings tab, which hosts the [SettingsGraph].
     */
    @Serializable
    data object Settings : AuthenticatorNavBarGraph()
}

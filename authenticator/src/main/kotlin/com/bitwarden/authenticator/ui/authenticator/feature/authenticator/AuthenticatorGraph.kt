package com.bitwarden.authenticator.ui.authenticator.feature.authenticator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Authenticator UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class AuthenticatorGraph : NavKey {
    /**
     * The type-safe route for the authenticator navigation bar screen, which hosts the
     * [com.bitwarden.authenticator.ui.authenticator.feature.navbar.AuthenticatorNavBarGraph].
     */
    @Serializable
    data object NavBarRoute : AuthenticatorGraph()

    /**
     * The type-safe route for the edit item screen.
     *
     * @property itemId ID of the item to be edited.
     */
    @Serializable
    data class EditItemRoute(
        val itemId: String,
    ) : AuthenticatorGraph()

    /**
     * The type-safe route for the item search screen.
     */
    @Serializable
    data object ItemSearchRoute : AuthenticatorGraph()

    /**
     * The type-safe route for the QR code scan screen.
     */
    @Serializable
    data object QrCodeScanRoute : AuthenticatorGraph()

    /**
     * The type-safe route for the manual code entry screen.
     */
    @Serializable
    data object ManualCodeEntryRoute : AuthenticatorGraph()

    /**
     * The type-safe route for the tutorial screen displayed from the settings.
     */
    @Serializable
    data object SettingsTutorialRoute : AuthenticatorGraph()
}

package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.navigation3.runtime.NavKey
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.AuthenticatorGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Root UI navigation graph and provides type-safe routes to each
 * top-level section of the app.
 */
@Serializable
sealed class RootNavGraph : NavKey {
    /**
     * The type-safe route for the splash screen.
     */
    @Serializable
    data object SplashRoute : RootNavGraph()

    /**
     * The type-safe route for the tutorial screen.
     */
    @Serializable
    data object TutorialRoute : RootNavGraph()

    /**
     * The type-safe route for the unlock screen.
     */
    @Serializable
    data object UnlockRoute : RootNavGraph()

    /**
     * The type-safe route for the authenticator section, which hosts the [AuthenticatorGraph].
     */
    @Serializable
    data object AuthenticatorRoute : RootNavGraph()
}

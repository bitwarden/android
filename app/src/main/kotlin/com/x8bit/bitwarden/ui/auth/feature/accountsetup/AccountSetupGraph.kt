package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Account Setup UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class AccountSetupGraph : NavKey {
    /**
     * The type-safe route for the setup unlock screen.
     */
    @Serializable
    data object SetupUnlockRoute : AccountSetupGraph()

    /**
     * The type-safe route for the setup autofill screen.
     */
    @Serializable
    data object SetupAutofillRoute : AccountSetupGraph()

    /**
     * The type-safe route for the setup browser autofill screen.
     */
    @Serializable
    data object SetupBrowserAutofillRoute : AccountSetupGraph()

    /**
     * The type-safe route for the setup complete screen.
     */
    @Serializable
    data object SetupCompleteRoute : AccountSetupGraph()
}

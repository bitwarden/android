package com.bitwarden.testharness.ui.platform.feature.rootnav

import androidx.navigation3.runtime.NavKey
import com.bitwarden.testharness.ui.platform.feature.autofill.AutofillGraph
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.CredentialManagerGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Root UI navigation graph and provides type-safe routes to each
 * top-level section of the test harness.
 */
@Serializable
sealed class RootNavGraph : NavKey {
    /**
     * The type-safe route for the landing screen, the main entry point showing the test category
     * options.
     */
    @Serializable
    data object LandingRoute : RootNavGraph()

    /**
     * The type-safe route for the Autofill test flow, which hosts the [AutofillGraph].
     */
    @Serializable
    data object AutofillRoute : RootNavGraph()

    /**
     * The type-safe route for the Credential Manager test flow, which hosts the
     * [CredentialManagerGraph].
     */
    @Serializable
    data object CredentialManagerRoute : RootNavGraph()
}

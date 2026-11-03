package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Credential Manager test UI navigation graph and provides
 * type-safe routes to each destination available in the graph.
 */
@Serializable
sealed class CredentialManagerGraph : NavKey {
    /**
     * The type-safe route for the Credential Manager test category list screen, the root of the
     * Credential Manager test flow.
     */
    @Serializable
    data object CredentialManagerListRoute : CredentialManagerGraph()

    /**
     * The type-safe route for the Get Password test screen.
     */
    @Serializable
    data object GetPasswordRoute : CredentialManagerGraph()

    /**
     * The type-safe route for the Create Password test screen.
     */
    @Serializable
    data object CreatePasswordRoute : CredentialManagerGraph()

    /**
     * The type-safe route for the Get Passkey test screen.
     */
    @Serializable
    data object GetPasskeyRoute : CredentialManagerGraph()

    /**
     * The type-safe route for the Create Passkey test screen.
     */
    @Serializable
    data object CreatePasskeyRoute : CredentialManagerGraph()

    /**
     * The type-safe route for the Get Password or Passkey test screen.
     */
    @Serializable
    data object GetPasswordOrPasskeyRoute : CredentialManagerGraph()
}

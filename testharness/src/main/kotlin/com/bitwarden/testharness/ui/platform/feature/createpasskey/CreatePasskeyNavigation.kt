@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.createpasskey

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.CredentialManagerGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry

/**
 * Add Create Passkey destination to the navigation entries.
 */
fun EntryProviderScope<CredentialManagerGraph>.createPasskeyDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<CredentialManagerGraph.CreatePasskeyRoute> {
        CreatePasskeyScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Create Passkey test screen.
 */
fun NavigationController<CredentialManagerGraph>.navigateToCreatePasskey() {
    this.navigate(route = CredentialManagerGraph.CreatePasskeyRoute)
}

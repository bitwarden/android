@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.createpassword

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.CredentialManagerGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry

/**
 * Add Create Password destination to the navigation entries.
 */
fun EntryProviderScope<CredentialManagerGraph>.createPasswordDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<CredentialManagerGraph.CreatePasswordRoute> {
        CreatePasswordScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Create Password test screen.
 */
fun NavigationController<CredentialManagerGraph>.navigateToCreatePassword() {
    this.navigate(route = CredentialManagerGraph.CreatePasswordRoute)
}

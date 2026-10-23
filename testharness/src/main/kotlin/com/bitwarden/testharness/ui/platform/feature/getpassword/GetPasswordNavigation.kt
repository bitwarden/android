@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.getpassword

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.CredentialManagerGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry

/**
 * Add Get Password destination to the navigation entries.
 */
fun EntryProviderScope<CredentialManagerGraph>.getPasswordDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<CredentialManagerGraph.GetPasswordRoute> {
        GetPasswordScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Get Password test screen.
 */
fun NavigationController<CredentialManagerGraph>.navigateToGetPassword() {
    this.navigate(route = CredentialManagerGraph.GetPasswordRoute)
}

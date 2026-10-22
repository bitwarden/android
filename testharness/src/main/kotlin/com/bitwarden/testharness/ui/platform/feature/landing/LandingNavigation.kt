@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.landing

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.ui.platform.navigation.util.rootPushEntry

/**
 * Add Landing destination to the navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.landingDestination(
    onNavigateToAutofill: () -> Unit,
    onNavigateToCredentialManager: () -> Unit,
) {
    rootPushEntry<RootNavGraph.LandingRoute> {
        LandingScreen(
            onNavigateToAutofill = onNavigateToAutofill,
            onNavigateToCredentialManager = onNavigateToCredentialManager,
        )
    }
}

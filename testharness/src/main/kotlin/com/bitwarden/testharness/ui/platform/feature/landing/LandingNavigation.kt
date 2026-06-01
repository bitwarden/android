@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.landing

import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import kotlinx.serialization.Serializable

/**
 * Landing screen - main entry point showing test category options.
 */
@Serializable
data object LandingRoute

/**
 * Add Landing destination to the nav graph.
 */
fun NavGraphBuilder.landingDestination(
    onNavigateToAutofill: () -> Unit,
    onNavigateToCredentialManager: () -> Unit,
) {
    composableWithRootPushTransitions<LandingRoute> {
        LandingScreen(
            onNavigateToAutofill = onNavigateToAutofill,
            onNavigateToCredentialManager = onNavigateToCredentialManager,
        )
    }
}

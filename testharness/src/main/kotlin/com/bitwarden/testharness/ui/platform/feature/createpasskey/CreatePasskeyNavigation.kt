@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.createpasskey

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * Create passkey test screen.
 */
@Serializable
data object CreatePasskeyRoute

/**
 * Add Create Passkey destination to the nav graph.
 */
fun NavGraphBuilder.createPasskeyDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<CreatePasskeyRoute> {
        CreatePasskeyScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Create Passkey test screen.
 */
fun NavController.navigateToCreatePasskey(navOptions: NavOptions? = null) {
    navigate(route = CreatePasskeyRoute, navOptions = navOptions)
}

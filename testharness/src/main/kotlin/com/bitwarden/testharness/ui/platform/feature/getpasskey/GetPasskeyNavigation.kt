@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.getpasskey

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * Get passkey test screen.
 */
@Serializable
data object GetPasskeyRoute

/**
 * Add Get Passkey destination to the nav graph.
 */
fun NavGraphBuilder.getPasskeyDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<GetPasskeyRoute> {
        GetPasskeyScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Get Passkey test screen.
 */
fun NavController.navigateToGetPasskey(navOptions: NavOptions? = null) {
    navigate(route = GetPasskeyRoute, navOptions = navOptions)
}

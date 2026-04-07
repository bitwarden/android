@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * Get Password or Passkey test screen.
 */
@Serializable
data object GetPasswordOrPasskeyRoute

/**
 * Add Get Password or Passkey destination to the nav graph.
 */
fun NavGraphBuilder.getPasswordOrPasskeyDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<GetPasswordOrPasskeyRoute> {
        GetPasswordOrPasskeyScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Get Password or Passkey test screen.
 */
fun NavController.navigateToGetPasswordOrPasskey(navOptions: NavOptions? = null) {
    navigate(route = GetPasswordOrPasskeyRoute, navOptions = navOptions)
}

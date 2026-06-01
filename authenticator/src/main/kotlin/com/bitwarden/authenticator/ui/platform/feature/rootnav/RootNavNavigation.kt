@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the root navigation screen.
 */
@Serializable
data object RootNavigationRoute

/**
 * Add the root navigation screen to the nav graph.
 */
fun NavGraphBuilder.rootNavDestination(
    onSplashScreenRemoved: () -> Unit,
) {
    composable<RootNavigationRoute> {
        RootNavScreen(onSplashScreenRemoved = onSplashScreenRemoved)
    }
}

@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.biometrics

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the Update Biometrics screen.
 */
@OmitFromCoverage
@Serializable
data object UpdateBiometricsRoute

/**
 * Add the Update Biometrics screen to the nav graph.
 */
fun NavGraphBuilder.updateBiometricsDestination(
    onDismiss: () -> Unit,
    onSplashScreenRemoved: () -> Unit,
) {
    composableWithSlideTransitions<UpdateBiometricsRoute> {
        UpdateBiometricsScreen(onDismiss = onDismiss)
        // If we are displaying the update biometrics screen, then we can just
        // hide the splash screen.
        onSplashScreenRemoved()
    }
}

/**
 * Navigate to the Update Biometrics screen.
 */
fun NavController.navigateToUpdateBiometrics() {
    this.navigate(route = UpdateBiometricsRoute) {
        launchSingleTop = true
    }
}

package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the trusted device screen.
 */
@Serializable
data object TrustedDeviceRoute

/**
 * Add the Trusted Device Screen to the nav graph.
 */
fun NavGraphBuilder.trustedDeviceDestination(
    onNavigateToAdminApproval: (emailAddress: String) -> Unit,
    onNavigateToLoginWithOtherDevice: (emailAddress: String) -> Unit,
    onNavigateToLock: (emailAddress: String) -> Unit,
) {
    composableWithSlideTransitions<TrustedDeviceRoute> {
        TrustedDeviceScreen(
            onNavigateToAdminApproval = onNavigateToAdminApproval,
            onNavigateToLoginWithOtherDevice = onNavigateToLoginWithOtherDevice,
            onNavigateToLock = onNavigateToLock,
        )
    }
}

/**
 * Navigate to the Trusted Device Screen.
 */
fun NavController.navigateToTrustedDevice(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = TrustedDeviceRoute, navOptions = navOptions)
}

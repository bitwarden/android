package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

/**
 * The route for navigating to the [TrustedDeviceScreen].
 */
const val TRUSTED_DEVICE_ROUTE: String = "trusted_device"

/**
 * Add the Trusted Device Screen to the nav graph.
 */
fun NavGraphBuilder.trustedDeviceDestination(
    onNavigateToAdminApproval: (emailAddress: String) -> Unit,
    onNavigateToLoginWithOtherDevice: (emailAddress: String) -> Unit,
    onNavigateToLock: (emailAddress: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = TRUSTED_DEVICE_ROUTE,
    ) {
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
    this.navigate(TRUSTED_DEVICE_ROUTE, navOptions)
}

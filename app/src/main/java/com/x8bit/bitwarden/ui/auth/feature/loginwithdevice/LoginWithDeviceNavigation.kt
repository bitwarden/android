package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val LOGIN_WITH_DEVICE_ROUTE = "login_with_device"

/**
 * Navigate to the Login with Device screen.
 */
fun NavController.navigateToLoginWithDevice(navOptions: NavOptions? = null) {
    this.navigate(LOGIN_WITH_DEVICE_ROUTE, navOptions)
}

/**
 * Add the Login with Device screen to the nav graph.
 */
fun NavGraphBuilder.loginWithDeviceDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = LOGIN_WITH_DEVICE_ROUTE,
    ) {
        LoginWithDeviceScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val LOGIN_WITH_DEVICE_PREFIX = "login_with_device"
private const val LOGIN_WITH_DEVICE_ROUTE = "$LOGIN_WITH_DEVICE_PREFIX/{$EMAIL_ADDRESS}"

/**
 * Class to retrieve login with device arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class LoginWithDeviceArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
    )
}

/**
 * Navigate to the Login with Device screen.
 */
fun NavController.navigateToLoginWithDevice(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("$LOGIN_WITH_DEVICE_PREFIX/$emailAddress", navOptions)
}

/**
 * Add the Login with Device screen to the nav graph.
 */
fun NavGraphBuilder.loginWithDeviceDestination(
    onNavigateBack: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = LOGIN_WITH_DEVICE_ROUTE,
    ) {
        LoginWithDeviceScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val LOGIN_WITH_DEVICE_PREFIX = "login_with_device"
private const val LOGIN_TYPE: String = "login_type"
private const val LOGIN_WITH_DEVICE_ROUTE =
    "$LOGIN_WITH_DEVICE_PREFIX/{$EMAIL_ADDRESS}/{$LOGIN_TYPE}"

/**
 * Class to retrieve login with device arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class LoginWithDeviceArgs(
    val emailAddress: String,
    val loginType: LoginWithDeviceType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle.get<String>(EMAIL_ADDRESS)),
        loginType = checkNotNull(savedStateHandle.get<LoginWithDeviceType>(LOGIN_TYPE)),
    )
}

/**
 * Navigate to the Login with Device screen.
 */
fun NavController.navigateToLoginWithDevice(
    emailAddress: String,
    loginType: LoginWithDeviceType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$LOGIN_WITH_DEVICE_PREFIX/$emailAddress/$loginType",
        navOptions = navOptions,
    )
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
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
            navArgument(LOGIN_TYPE) { type = NavType.EnumType(LoginWithDeviceType::class.java) },
        ),
    ) {
        LoginWithDeviceScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

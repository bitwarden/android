package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the login with device screen.
 */
@Serializable
data class LoginWithDeviceRoute(
    val emailAddress: String,
    val loginType: LoginWithDeviceType,
)

/**
 * Class to retrieve login with device arguments from the [SavedStateHandle].
 */
data class LoginWithDeviceArgs(
    val emailAddress: String,
    val loginType: LoginWithDeviceType,
)

/**
 * Constructs a [LoginWithDeviceArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toLoginWithDeviceArgs(): LoginWithDeviceArgs {
    val route = this.toRoute<LoginWithDeviceRoute>()
    return LoginWithDeviceArgs(emailAddress = route.emailAddress, loginType = route.loginType)
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
        route = LoginWithDeviceRoute(
            emailAddress = emailAddress,
            loginType = loginType,
        ),
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
    composableWithSlideTransitions<LoginWithDeviceRoute> {
        LoginWithDeviceScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

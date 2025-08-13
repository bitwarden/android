package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the login screen.
 */
@Serializable
data class LoginRoute(
    val emailAddress: String,
)

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
data class LoginArgs(val emailAddress: String)

/**
 * Constructs a [LoginArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toLoginArgs(): LoginArgs {
    val route = this.toRoute<LoginRoute>()
    return LoginArgs(
        emailAddress = route.emailAddress,
    )
}

/**
 * Navigate to the login screen with the given email address and region label.
 */
fun NavController.navigateToLogin(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = LoginRoute(emailAddress = emailAddress),
        navOptions = navOptions,
    )
}

/**
 * Add the Login screen to the nav graph.
 */
fun NavGraphBuilder.loginDestination(
    onNavigateBack: () -> Unit,
    onNavigateToMasterPasswordHint: (emailAddress: String) -> Unit,
    onNavigateToEnterpriseSignOn: (emailAddress: String) -> Unit,
    onNavigateToLoginWithDevice: (emailAddress: String) -> Unit,
    onNavigateToTwoFactorLogin: (
        emailAddress: String,
        password: String?,
        isNewDeviceVerification: Boolean,
    ) -> Unit,
) {
    composableWithSlideTransitions<LoginRoute> {
        LoginScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToMasterPasswordHint = onNavigateToMasterPasswordHint,
            onNavigateToEnterpriseSignOn = onNavigateToEnterpriseSignOn,
            onNavigateToLoginWithDevice = onNavigateToLoginWithDevice,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

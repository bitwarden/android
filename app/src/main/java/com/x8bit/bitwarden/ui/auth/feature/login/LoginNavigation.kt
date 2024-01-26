package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val CAPTCHA_TOKEN = "captcha_token"
private const val LOGIN_ROUTE: String = "login/{$EMAIL_ADDRESS}?$CAPTCHA_TOKEN={$CAPTCHA_TOKEN}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class LoginArgs(val emailAddress: String, val captchaToken: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
        savedStateHandle[CAPTCHA_TOKEN],
    )
}

/**
 * Navigate to the login screen with the given email address and region label.
 */
fun NavController.navigateToLogin(
    emailAddress: String,
    captchaToken: String?,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        "login/$emailAddress?$CAPTCHA_TOKEN=$captchaToken",
        navOptions,
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
    onNavigateToTwoFactorLogin: (emailAddress: String, password: String?) -> Unit,
) {
    composableWithSlideTransitions(
        route = LOGIN_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
            navArgument(CAPTCHA_TOKEN) {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        LoginScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToMasterPasswordHint = onNavigateToMasterPasswordHint,
            onNavigateToEnterpriseSignOn = onNavigateToEnterpriseSignOn,
            onNavigateToLoginWithDevice = onNavigateToLoginWithDevice,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val EMAIL_ADDRESS: String = "email_address"
private const val CAPTCHA_TOKEN = "captcha_token"
private const val LOGIN_ROUTE: String = "login/{$EMAIL_ADDRESS}/{$CAPTCHA_TOKEN}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
class LoginArgs(val emailAddress: String, val captchaToken: String?) {
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
    this.navigate("login/$emailAddress/$captchaToken", navOptions)
}

/**
 * Add the Login screen to the nav graph.
 */
fun NavGraphBuilder.loginDestinations(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = LOGIN_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        LoginScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

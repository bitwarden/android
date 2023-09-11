package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val EMAIL_ADDRESS: String = "email_address"
private const val LOGIN_ROUTE: String = "login/{$EMAIL_ADDRESS}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
class LoginArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
    )
}

/**
 * Navigate to the login screen with the given email address.
 */
fun NavController.navigateToLogin(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("login/$emailAddress", navOptions)
}

/**
 * Add the Login screen to the nav graph.
 */
fun NavGraphBuilder.loginDestinations(
    onNavigateToLanding: () -> Unit,
) {
    composable(
        route = LOGIN_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        LoginScreen(
            onNavigateToLanding = { onNavigateToLanding() },
        )
    }
}

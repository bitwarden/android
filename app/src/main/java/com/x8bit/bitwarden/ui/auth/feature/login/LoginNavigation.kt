package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val EMAIL_ADDRESS: String = "email_address"
private const val REGION_LABEL: String = "region_label"
private const val LOGIN_ROUTE: String = "login/{$EMAIL_ADDRESS}/{$REGION_LABEL}"

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
class LoginArgs(val emailAddress: String, val regionLabel: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
        checkNotNull(savedStateHandle[REGION_LABEL]) as String,
    )
}

/**
 * Navigate to the login screen with the given email address and region label.
 */
fun NavController.navigateToLogin(
    emailAddress: String,
    regionLabel: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("login/$emailAddress/$regionLabel", navOptions)
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

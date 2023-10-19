package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

private const val CREATE_ACCOUNT_ROUTE = "create_account"

/**
 * Navigate to the create account screen.
 */
fun NavController.navigateToCreateAccount(navOptions: NavOptions? = null) {
    this.navigate(CREATE_ACCOUNT_ROUTE, navOptions)
}

/**
 * Add the create account screen to the nav graph.
 */
fun NavGraphBuilder.createAccountDestinations(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (emailAddress: String, captchaToken: String) -> Unit,
) {
    composable(route = CREATE_ACCOUNT_ROUTE) {
        CreateAccountScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

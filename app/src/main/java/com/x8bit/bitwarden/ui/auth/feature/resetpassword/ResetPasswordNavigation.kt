package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val RESET_PASSWORD_ROUTE: String = "reset_password"

/**
 * Add the Reset Password screen to the nav graph.
 */
fun NavGraphBuilder.resetPasswordDestination(
    onNavigateToPreventAccountLockOut: () -> Unit,
) {
    composable(
        route = RESET_PASSWORD_ROUTE,
    ) {
        ResetPasswordScreen(onNavigateToPreventAccountLockOut = onNavigateToPreventAccountLockOut)
    }
}

/**
 * Navigate to the Reset Password screen.
 */
fun NavController.navigateToResetPasswordScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(RESET_PASSWORD_ROUTE, navOptions)
}

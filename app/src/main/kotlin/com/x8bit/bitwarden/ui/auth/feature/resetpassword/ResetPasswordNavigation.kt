package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the reset password screen.
 */
@Serializable
data object ResetPasswordRoute

/**
 * Add the Reset Password screen to the nav graph.
 */
fun NavGraphBuilder.resetPasswordDestination(
    onNavigateToPreventAccountLockOut: () -> Unit,
) {
    composable<ResetPasswordRoute> {
        ResetPasswordScreen(onNavigateToPreventAccountLockOut = onNavigateToPreventAccountLockOut)
    }
}

/**
 * Navigate to the Reset Password screen.
 */
fun NavController.navigateToResetPasswordScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = ResetPasswordRoute, navOptions = navOptions)
}

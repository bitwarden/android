package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.navigateToPreventAccountLockout
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.preventAccountLockoutDestination
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the reset password graph.
 */
@Serializable
data object ResetPasswordGraphRoute

/**
 * The type-safe route for the reset password screen.
 */
@Serializable
data object ResetPasswordRoute

/**
 * Add password reset destinations to the nav graph.
 */
fun NavGraphBuilder.passwordResetGraph(navController: NavHostController) {
    navigation<ResetPasswordGraphRoute>(
        startDestination = ResetPasswordRoute,
    ) {
        resetPasswordDestination(
            onNavigateToPreventAccountLockOut = {
                navController.navigateToPreventAccountLockout(isPasswordReset = true)
            },
        )
        preventAccountLockoutDestination(
            isPasswordReset = true,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

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
 * Navigate to the Reset Password graph.
 */
fun NavController.navigateToResetPasswordGraph(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = ResetPasswordGraphRoute, navOptions = navOptions)
}

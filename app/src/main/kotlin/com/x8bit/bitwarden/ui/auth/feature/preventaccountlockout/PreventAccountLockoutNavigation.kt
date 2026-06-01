package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the prevent account lockout screen.
 */
@Serializable
sealed class PreventAccountLockoutRoute {
    /**
     * The type-safe route for the prevent account lockout screen.
     */
    @Serializable
    data object Standard : PreventAccountLockoutRoute()

    /**
     * The type-safe route for the password reset prevent account lockout screen.
     */
    @Serializable
    data object PasswordReset : PreventAccountLockoutRoute()
}

/**
 * Navigate to prevent account lockout screen.
 */
fun NavController.navigateToPreventAccountLockout(
    isPasswordReset: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = if (isPasswordReset) {
            PreventAccountLockoutRoute.PasswordReset
        } else {
            PreventAccountLockoutRoute.Standard
        },
        navOptions = navOptions,
    )
}

/**
 * Add the prevent account lockout screen to the nav graph.
 */
fun NavGraphBuilder.preventAccountLockoutDestination(
    isPasswordReset: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPasswordReset) {
        composableWithSlideTransitions<PreventAccountLockoutRoute.PasswordReset> {
            PreventAccountLockoutScreen(onNavigateBack = onNavigateBack)
        }
    } else {
        composableWithSlideTransitions<PreventAccountLockoutRoute.Standard> {
            PreventAccountLockoutScreen(onNavigateBack = onNavigateBack)
        }
    }
}

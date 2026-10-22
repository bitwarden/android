package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Reset Password UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class ResetPasswordGraph : NavKey {
    /**
     * The type-safe route for the reset password screen.
     */
    @Serializable
    data object ResetPasswordRoute : ResetPasswordGraph()

    /**
     * The type-safe route for the prevent account lockout screen.
     */
    @Serializable
    data object PreventAccountLockoutRoute : ResetPasswordGraph()
}

package com.x8bit.bitwarden.ui.auth.feature.setpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the set password screen.
 */
@Serializable
data object SetPasswordRoute

/**
 * Add the Set Password screen to the nav graph.
 */
fun NavGraphBuilder.setPasswordDestination() {
    composable<SetPasswordRoute> {
        SetPasswordScreen()
    }
}

/**
 * Navigate to the Set Password screen.
 */
fun NavController.navigateToSetPassword(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = SetPasswordRoute, navOptions = navOptions)
}

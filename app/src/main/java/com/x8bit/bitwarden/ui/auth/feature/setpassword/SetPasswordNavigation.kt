package com.x8bit.bitwarden.ui.auth.feature.setpassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val SET_PASSWORD_ROUTE: String = "set_password"

/**
 * Add the Set Password screen to the nav graph.
 */
fun NavGraphBuilder.setPasswordDestination() {
    composable(
        route = SET_PASSWORD_ROUTE,
    ) {
        SetPasswordScreen()
    }
}

/**
 * Navigate to the Set Password screen.
 */
fun NavController.navigateToSetPassword(
    navOptions: NavOptions? = null,
) {
    this.navigate(SET_PASSWORD_ROUTE, navOptions)
}

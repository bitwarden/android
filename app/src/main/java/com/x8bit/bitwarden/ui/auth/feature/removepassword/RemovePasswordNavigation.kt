package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

/**
 * The route for navigating to the [RemovePasswordScreen].
 */
const val REMOVE_PASSWORD_ROUTE: String = "remove_password"

/**
 * Add the Remove Password screen to the nav graph.
 */
fun NavGraphBuilder.removePasswordDestination() {
    composable(
        route = REMOVE_PASSWORD_ROUTE,
    ) {
        RemovePasswordScreen()
    }
}

/**
 * Navigate to the Remove Password screen.
 */
fun NavController.navigateToRemovePassword(
    navOptions: NavOptions? = null,
) {
    this.navigate(REMOVE_PASSWORD_ROUTE, navOptions)
}

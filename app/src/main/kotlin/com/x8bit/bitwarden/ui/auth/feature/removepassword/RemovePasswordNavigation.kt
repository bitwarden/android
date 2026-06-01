package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the remove password screen.
 */
@Serializable
data object RemovePasswordRoute

/**
 * Add the Remove Password screen to the nav graph.
 */
fun NavGraphBuilder.removePasswordDestination() {
    composable<RemovePasswordRoute> {
        RemovePasswordScreen()
    }
}

/**
 * Navigate to the Remove Password screen.
 */
fun NavController.navigateToRemovePassword(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = RemovePasswordRoute, navOptions = navOptions)
}

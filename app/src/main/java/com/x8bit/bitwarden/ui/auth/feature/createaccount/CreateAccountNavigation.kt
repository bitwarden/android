package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the create account screen.
 */
@Serializable
data object CreateAccountRoute

/**
 * Navigate to the create account screen.
 */
fun NavController.navigateToCreateAccount(navOptions: NavOptions? = null) {
    this.navigate(route = CreateAccountRoute, navOptions = navOptions)
}

/**
 * Add the create account screen to the nav graph.
 */
fun NavGraphBuilder.createAccountDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (emailAddress: String, captchaToken: String?) -> Unit,
) {
    composableWithSlideTransitions<CreateAccountRoute> {
        CreateAccountScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

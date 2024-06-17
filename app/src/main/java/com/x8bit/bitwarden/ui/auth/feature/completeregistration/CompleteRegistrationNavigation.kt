package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val COMPLETE_REGISTRATION_ROUTE = "complete_registration"

/**
 * Navigate to the complete registration screen.
 */
fun NavController.navigateToCompleteRegistration(navOptions: NavOptions? = null) {
    this.navigate(COMPLETE_REGISTRATION_ROUTE, navOptions)
}

/**
 * Add the complete registration screen to the nav graph.
 */
fun NavGraphBuilder.completeRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (emailAddress: String, captchaToken: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = COMPLETE_REGISTRATION_ROUTE,
    ) {
        CompleteRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}

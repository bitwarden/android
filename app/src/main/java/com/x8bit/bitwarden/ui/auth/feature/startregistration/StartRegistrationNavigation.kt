package com.x8bit.bitwarden.ui.auth.feature.startregistration

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val START_REGISTRATION_ROUTE = "start_registration"

/**
 * Navigate to the start registration screen.
 */
fun NavController.navigateToStartRegistration(navOptions: NavOptions? = null) {
    this.navigate(START_REGISTRATION_ROUTE, navOptions)
}

/**
 * Add the start registration screen to the nav graph.
 */
fun NavGraphBuilder.startRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToCompleteRegistration: (
        emailAddress: String,
        verificationToken: String,
    ) -> Unit,
    onNavigateToCheckEmail: (email: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
) {
    composableWithSlideTransitions(
        route = START_REGISTRATION_ROUTE,
    ) {
        StartRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToCompleteRegistration = onNavigateToCompleteRegistration,
            onNavigateToCheckEmail = onNavigateToCheckEmail,
            onNavigateToEnvironment = onNavigateToEnvironment,
        )
    }
}

package com.x8bit.bitwarden.ui.auth.feature.startregistration

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the start registration screen.
 */
@Serializable
data object StartRegistrationRoute

/**
 * Navigate to the start registration screen.
 */
fun NavController.navigateToStartRegistration(navOptions: NavOptions? = null) {
    this.navigate(route = StartRegistrationRoute, navOptions = navOptions)
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
    composableWithSlideTransitions<StartRegistrationRoute> {
        StartRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToCompleteRegistration = onNavigateToCompleteRegistration,
            onNavigateToCheckEmail = onNavigateToCheckEmail,
            onNavigateToEnvironment = onNavigateToEnvironment,
        )
    }
}

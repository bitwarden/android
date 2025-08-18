package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the complete registration screen.
 */
@Serializable
data class CompleteRegistrationRoute(
    val emailAddress: String,
    val verificationToken: String,
    val fromEmail: Boolean,
)

/**
 * Class to retrieve complete registration arguments from the [SavedStateHandle].
 */
data class CompleteRegistrationArgs(
    val emailAddress: String,
    val verificationToken: String,
    val fromEmail: Boolean,
)

/**
 * Constructs a [CompleteRegistrationArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toCompleteRegistrationArgs(): CompleteRegistrationArgs {
    val route = this.toRoute<CompleteRegistrationRoute>()
    return CompleteRegistrationArgs(
        emailAddress = route.emailAddress,
        verificationToken = route.verificationToken,
        fromEmail = route.fromEmail,
    )
}

/**
 * Navigate to the complete registration screen.
 */
fun NavController.navigateToCompleteRegistration(
    emailAddress: String,
    verificationToken: String,
    fromEmail: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = CompleteRegistrationRoute(
            emailAddress = emailAddress,
            verificationToken = verificationToken,
            fromEmail = fromEmail,
        ),
        navOptions = navOptions,
    )
}

/**
 * Add the complete registration screen to the nav graph.
 */
fun NavGraphBuilder.completeRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPasswordGuidance: () -> Unit,
    onNavigateToPreventAccountLockout: () -> Unit,
    onNavigateToLogin: (email: String) -> Unit,
) {
    composableWithSlideTransitions<CompleteRegistrationRoute> {
        CompleteRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPasswordGuidance = onNavigateToPasswordGuidance,
            onNavigateToPreventAccountLockout = onNavigateToPreventAccountLockout,
            onNavigateToLogin = onNavigateToLogin,
        )
    }
}

/**
 * Pop up to the complete registration screen.
 */
fun NavController.popUpToCompleteRegistration() {
    this.popBackStack(route = CompleteRegistrationRoute, inclusive = false)
}

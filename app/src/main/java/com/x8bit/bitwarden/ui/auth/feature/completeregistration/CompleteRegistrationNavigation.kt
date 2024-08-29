package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val VERIFICATION_TOKEN: String = "verification_token"
private const val FROM_EMAIL: String = "from_email"
private const val COMPLETE_REGISTRATION_PREFIX = "complete_registration"
private const val COMPLETE_REGISTRATION_ROUTE =
    "$COMPLETE_REGISTRATION_PREFIX/{$EMAIL_ADDRESS}/{$VERIFICATION_TOKEN}/{$FROM_EMAIL}"

/**
 * Class to retrieve complete registration arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class CompleteRegistrationArgs(
    val emailAddress: String,
    val verificationToken: String,
    val fromEmail: Boolean,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle.get<String>(EMAIL_ADDRESS)),
        verificationToken = checkNotNull(savedStateHandle.get<String>(VERIFICATION_TOKEN)),
        fromEmail = checkNotNull(savedStateHandle.get<Boolean>(FROM_EMAIL)),
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
        "$COMPLETE_REGISTRATION_PREFIX/$emailAddress/$verificationToken/$fromEmail",
        navOptions,
    )
}

/**
 * Add the complete registration screen to the nav graph.
 */
fun NavGraphBuilder.completeRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPasswordGuidance: () -> Unit,
    onNavigateToPreventAccountLockout: () -> Unit,
    onNavigateToLogin: (email: String, token: String?) -> Unit,
) {
    composableWithSlideTransitions(
        route = COMPLETE_REGISTRATION_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
            navArgument(VERIFICATION_TOKEN) { type = NavType.StringType },
            navArgument(FROM_EMAIL) { type = NavType.BoolType },
        ),
    ) {
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
    popBackStack(route = COMPLETE_REGISTRATION_ROUTE, inclusive = false)
}

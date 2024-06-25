package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val VERIFICATION_TOKEN: String = "verification_token"
private const val REGION: String = "region"
private const val COMPLETE_REGISTRATION_PREFIX = "complete_registration"
private const val COMPLETE_REGISTRATION_ROUTE = "$COMPLETE_REGISTRATION_PREFIX/{$EMAIL_ADDRESS}/{$VERIFICATION_TOKEN}/{$REGION}"

/**
 * Class to retrieve login with device arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class CompleteRegistrationArgs(
    val emailAddress: String,
    val verificationToken: String,
    val region: Environment.Type
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle.get<String>(EMAIL_ADDRESS)),
        verificationToken = checkNotNull(savedStateHandle.get<String>(VERIFICATION_TOKEN)),
        region = checkNotNull(savedStateHandle.get<Environment.Type>(REGION))
    )
}

/**
 * Navigate to the complete registration screen.
 */
fun NavController.navigateToCompleteRegistration(
    emailAddress: String,
    verificationToken: String,
    region: Environment.Type? = null,
    navOptions: NavOptions? = null) {
    this.navigate("$COMPLETE_REGISTRATION_PREFIX/$emailAddress/$verificationToken/$region", navOptions)
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
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
            navArgument(VERIFICATION_TOKEN) { type = NavType.StringType },
            navArgument(REGION) { type = NavType.EnumType(Environment.Type::class.java) },
        ),
    ) {
        CompleteRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}

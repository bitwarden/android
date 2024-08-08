package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL: String = "email"
private const val CHECK_EMAIL_ROUTE: String = "check_email/{$EMAIL}"

/**
 * Navigate to the check email screen.
 */
fun NavController.navigateToCheckEmail(emailAddress: String, navOptions: NavOptions? = null) {
    this.navigate("check_email/$emailAddress", navOptions)
}

/**
 * Class to retrieve check email arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class CheckEmailArgs(
    val emailAddress: String,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle.get<String>(EMAIL)),
    )
}

/**
 * Add the check email screen to the nav graph.
 */
fun NavGraphBuilder.checkEmailDestination(
    onNavigateBack: () -> Unit,
    onNavigateBackToLanding: () -> Unit,
) {
    composableWithSlideTransitions(
        route = CHECK_EMAIL_ROUTE,
        arguments = listOf(
            navArgument(EMAIL) { type = NavType.StringType },
        ),
    ) {
        CheckEmailScreen(
            onNavigateBack = onNavigateBack,
            onNavigateBackToLanding = onNavigateBackToLanding,
        )
    }
}

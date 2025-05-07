package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the check email screen.
 */
@Serializable
data class CheckEmailRoute(
    val emailAddress: String,
)

/**
 * Navigate to the check email screen.
 */
fun NavController.navigateToCheckEmail(emailAddress: String, navOptions: NavOptions? = null) {
    this.navigate(route = CheckEmailRoute(emailAddress = emailAddress), navOptions = navOptions)
}

/**
 * Class to retrieve check email arguments from the [SavedStateHandle].
 */
data class CheckEmailArgs(
    val emailAddress: String,
)

/**
 * Constructs a [CheckEmailArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toCheckEmailArgs(): CheckEmailArgs {
    val route = this.toRoute<CheckEmailRoute>()
    return CheckEmailArgs(emailAddress = route.emailAddress)
}

/**
 * Add the check email screen to the nav graph.
 */
fun NavGraphBuilder.checkEmailDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<CheckEmailRoute> {
        CheckEmailScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

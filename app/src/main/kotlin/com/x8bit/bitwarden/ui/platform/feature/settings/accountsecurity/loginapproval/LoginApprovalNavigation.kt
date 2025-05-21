package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the login approval screen.
 */
@Serializable
data class LoginApprovalRoute(
    val fingerprint: String?,
)

/**
 * Class to retrieve login approval arguments from the [SavedStateHandle].
 */
data class LoginApprovalArgs(val fingerprint: String?)

/**
 * Constructs a [LoginApprovalArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toLoginApprovalArgs(): LoginApprovalArgs {
    val route = this.toRoute<LoginApprovalRoute>()
    return LoginApprovalArgs(fingerprint = route.fingerprint)
}

/**
 * Add login approval destinations to the nav graph.
 */
fun NavGraphBuilder.loginApprovalDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<LoginApprovalRoute> {
        LoginApprovalScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Login Approval screen.
 */
fun NavController.navigateToLoginApproval(
    fingerprint: String?,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = LoginApprovalRoute(fingerprint = fingerprint), navOptions = navOptions)
}

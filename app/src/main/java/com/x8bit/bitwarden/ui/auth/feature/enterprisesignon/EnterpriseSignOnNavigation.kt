package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the enterprise sign-on screen.
 */
@Serializable
data class EnterpriseSignOnRoute(
    val emailAddress: String,
)

/**
 * Class to retrieve login arguments from the [SavedStateHandle].
 */
data class EnterpriseSignOnArgs(val emailAddress: String)

/**
 * Constructs a [EnterpriseSignOnArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toEnterpriseSignOnArgs(): EnterpriseSignOnArgs {
    val route = this.toRoute<EnterpriseSignOnRoute>()
    return EnterpriseSignOnArgs(emailAddress = route.emailAddress)
}

/**
 * Navigate to the enterprise single sign on screen.
 */
fun NavController.navigateToEnterpriseSignOn(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = EnterpriseSignOnRoute(emailAddress = emailAddress),
        navOptions = navOptions,
    )
}

/**
 * Add the enterprise sign on screen to the nav graph.
 */
fun NavGraphBuilder.enterpriseSignOnDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSetPassword: () -> Unit,
    onNavigateToTwoFactorLogin: (emailAddress: String, orgIdentifier: String) -> Unit,
) {
    composableWithSlideTransitions<EnterpriseSignOnRoute> {
        EnterpriseSignOnScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSetPassword = onNavigateToSetPassword,
            onNavigateToTwoFactorLogin = onNavigateToTwoFactorLogin,
        )
    }
}

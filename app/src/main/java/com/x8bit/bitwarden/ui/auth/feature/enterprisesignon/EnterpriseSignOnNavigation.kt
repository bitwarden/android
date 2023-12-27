package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val ENTERPRISE_SIGN_ON_ROUTE = "enterprise_sign_on"

/**
 * Navigate to the enterprise single sign on screen.
 */
fun NavController.navigateToEnterpriseSignOn(navOptions: NavOptions? = null) {
    this.navigate(ENTERPRISE_SIGN_ON_ROUTE, navOptions)
}

/**
 * Add the enterprise sign on screen to the nav graph.
 */
fun NavGraphBuilder.enterpriseSignOnDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = ENTERPRISE_SIGN_ON_ROUTE,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.stay,
        popEnterTransition = TransitionProviders.Enter.stay,
        popExitTransition = TransitionProviders.Exit.slideDown,
    ) {
        EnterpriseSignOnScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

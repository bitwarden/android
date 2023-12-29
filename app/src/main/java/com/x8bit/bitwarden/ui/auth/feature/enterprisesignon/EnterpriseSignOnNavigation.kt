package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

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
    composableWithSlideTransitions(
        route = ENTERPRISE_SIGN_ON_ROUTE,
    ) {
        EnterpriseSignOnScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

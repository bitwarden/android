package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

private const val ACCOUNT_SECURITY_ROUTE = "settings_account_security"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.accountSecurityDestination(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = ACCOUNT_SECURITY_ROUTE,
        enterTransition = TransitionProviders.Enter.pushLeft,
        exitTransition = TransitionProviders.Exit.pushLeft,
        popEnterTransition = TransitionProviders.Enter.pushLeft,
        popExitTransition = TransitionProviders.Exit.pushRight,
    ) {
        AccountSecurityScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the account security screen.
 */
fun NavController.navigateToAccountSecurity(navOptions: NavOptions? = null) {
    navigate(ACCOUNT_SECURITY_ROUTE, navOptions)
}

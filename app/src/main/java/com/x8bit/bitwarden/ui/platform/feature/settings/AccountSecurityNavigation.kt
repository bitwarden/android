package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

private const val ACCOUNT_SECURITY_ROUTE = "account_security"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.accountSecurityDestination(
    onNavigateBack: () -> Unit,
) {
    composable(ACCOUNT_SECURITY_ROUTE) {
        AccountSecurityScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the account security screen.
 */
fun NavController.navigateToAccountSecurity(navOptions: NavOptions? = null) {
    navigate(ACCOUNT_SECURITY_ROUTE, navOptions)
}

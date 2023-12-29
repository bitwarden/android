package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val ACCOUNT_SECURITY_ROUTE = "settings_account_security"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.accountSecurityDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
) {
    composableWithPushTransitions(
        route = ACCOUNT_SECURITY_ROUTE,
    ) {
        AccountSecurityScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
        )
    }
}

/**
 * Navigate to the account security screen.
 */
fun NavController.navigateToAccountSecurity(navOptions: NavOptions? = null) {
    navigate(ACCOUNT_SECURITY_ROUTE, navOptions)
}

package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the account security screen.
 */
@Serializable
data object AccountSecurityRoute

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.accountSecurityDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
) {
    composableWithPushTransitions<AccountSecurityRoute> {
        AccountSecurityScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDeleteAccount = onNavigateToDeleteAccount,
            onNavigateToPendingRequests = onNavigateToPendingRequests,
            onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
        )
    }
}

/**
 * Navigate to the account security screen.
 */
fun NavController.navigateToAccountSecurity(navOptions: NavOptions? = null) {
    this.navigate(route = AccountSecurityRoute, navOptions = navOptions)
}

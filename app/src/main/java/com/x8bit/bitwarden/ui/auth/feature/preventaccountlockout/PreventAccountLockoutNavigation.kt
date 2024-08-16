package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val PREVENT_ACCOUNT_LOCKOUT = "prevent_account_lockout"

/**
 * Navigate to prevent account lockout screen.
 */
fun NavController.navigateToPreventAccountLockout(navOptions: NavOptions? = null) {
    this.navigate(PREVENT_ACCOUNT_LOCKOUT, navOptions)
}

/**
 * Add the prevent account lockout screen to the nav graph.
 */
fun NavGraphBuilder.preventAccountLockoutDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = PREVENT_ACCOUNT_LOCKOUT,
    ) {
        PreventAccountLockoutScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

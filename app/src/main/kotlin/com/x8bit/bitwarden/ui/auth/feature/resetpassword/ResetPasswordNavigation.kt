package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootTransitionEntry
import com.bitwarden.ui.platform.theme.RootTransition
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.navigateToResetPasswordPreventAccountLockout
import com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout.resetPasswordPreventAccountLockoutDestination
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph

/**
 * Add the reset password section to the root navigation entries. The
 * [RootNavGraph.ResetPasswordRoute] hosts the [ResetPasswordGraph].
 */
fun EntryProviderScope<RootNavGraph>.resetPasswordGraph() {
    rootTransitionEntry<RootNavGraph.ResetPasswordRoute>(rootTransition = RootTransition.SLIDE_UP) {
        val navigationController = rememberNavigationController<ResetPasswordGraph>(
            routes = arrayOf(ResetPasswordGraph.ResetPasswordRoute),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                resetPasswordDestination(
                    onNavigateToPreventAccountLockOut = {
                        navigationController.navigateToResetPasswordPreventAccountLockout()
                    },
                )
                resetPasswordPreventAccountLockoutDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
            },
        )
    }
}

private fun EntryProviderScope<ResetPasswordGraph>.resetPasswordDestination(
    onNavigateToPreventAccountLockOut: () -> Unit,
) {
    entry<ResetPasswordGraph.ResetPasswordRoute> {
        ResetPasswordScreen(onNavigateToPreventAccountLockOut = onNavigateToPreventAccountLockOut)
    }
}

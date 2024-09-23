package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

/**
 * Route name for [SetupCompleteScreen].
 */
const val SETUP_COMPLETE_ROUTE = "setup_complete"

/**
 * Navigate to the setup complete screen.
 */
fun NavController.navigateToSetupCompleteScreen(navOptions: NavOptions? = null) {
    this.navigate(SETUP_COMPLETE_ROUTE, navOptions)
}

/**
 * Add the setup complete screen to the nav graph.
 */
fun NavGraphBuilder.setupCompleteDestination() {
    composableWithPushTransitions(
        route = SETUP_COMPLETE_ROUTE,
    ) {
        SetupCompleteScreen()
    }
}

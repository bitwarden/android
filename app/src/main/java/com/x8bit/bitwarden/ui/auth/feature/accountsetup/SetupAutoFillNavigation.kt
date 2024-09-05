package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

/**
 * Route name for [SetupAutoFillScreen].
 */
const val SETUP_AUTO_FILL_ROUTE = "setup_auto_fill"

/**
 * Navigate to the setup auto-fill screen.
 */
fun NavController.navigateToSetupAutoFillScreen(navOptions: NavOptions? = null) {
    this.navigate(SETUP_AUTO_FILL_ROUTE, navOptions)
}

/**
 * Add the setup auto-fil screen to the nav graph.
 */
fun NavGraphBuilder.setupAutoFillDestination() {
    composableWithPushTransitions(
        route = SETUP_AUTO_FILL_ROUTE,
    ) {
        SetupAutoFillScreen()
    }
}

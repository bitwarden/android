package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

/**
 * Route for [SetupUnlockScreen]
 */
const val SETUP_UNLOCK_ROUTE = "setup_unlock"

/**
 * Navigate to the setup unlock screen.
 */
fun NavController.navigateToSetupUnlockScreen(navOptions: NavOptions? = null) {
    this.navigate(SETUP_UNLOCK_ROUTE, navOptions)
}

/**
 * Add the setup unlock screen to the nav graph.
 */
fun NavGraphBuilder.setupUnlockDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = SETUP_UNLOCK_ROUTE,
    ) {
        SetupUnlockScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

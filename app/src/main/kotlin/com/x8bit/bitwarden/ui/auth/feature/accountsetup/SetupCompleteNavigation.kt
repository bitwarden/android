package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the setup complete screen.
 */
@Serializable
data object SetupCompleteRoute

/**
 * Navigate to the setup complete screen.
 */
fun NavController.navigateToSetupCompleteScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupCompleteRoute, navOptions = navOptions)
}

/**
 * Add the setup complete screen to the nav graph.
 */
fun NavGraphBuilder.setupCompleteDestination() {
    composableWithPushTransitions<SetupCompleteRoute> {
        SetupCompleteScreen()
    }
}

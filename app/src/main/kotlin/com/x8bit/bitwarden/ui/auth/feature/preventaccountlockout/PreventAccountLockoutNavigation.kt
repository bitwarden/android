package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the prevent account lockout screen.
 */
@Serializable
data object PreventAccountLockoutRoute

/**
 * Navigate to prevent account lockout screen.
 */
fun NavController.navigateToPreventAccountLockout(navOptions: NavOptions? = null) {
    this.navigate(route = PreventAccountLockoutRoute, navOptions = navOptions)
}

/**
 * Add the prevent account lockout screen to the nav graph.
 */
fun NavGraphBuilder.preventAccountLockoutDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<PreventAccountLockoutRoute> {
        PreventAccountLockoutScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

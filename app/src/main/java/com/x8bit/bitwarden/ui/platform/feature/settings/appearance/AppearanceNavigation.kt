package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val PRE_AUTH_APPEARANCE_ROUTE = "pre_auth_settings_appearance"
private const val APPEARANCE_ROUTE = "settings_appearance"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.appearanceDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = getRoute(isPreAuth = isPreAuth),
    ) {
        AppearanceScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the appearance screen.
 */
fun NavController.navigateToAppearance(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = getRoute(isPreAuth = isPreAuth), navOptions = navOptions)
}

private fun getRoute(
    isPreAuth: Boolean,
): String = if (isPreAuth) PRE_AUTH_APPEARANCE_ROUTE else APPEARANCE_ROUTE

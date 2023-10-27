package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

private const val APPEARANCE_ROUTE = "settings_appearance"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.appearanceDestination(
    onNavigateBack: () -> Unit,
) {
    composable(APPEARANCE_ROUTE) {
        AppearanceScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the appearance screen.
 */
fun NavController.navigateToAppearance(navOptions: NavOptions? = null) {
    navigate(APPEARANCE_ROUTE, navOptions)
}

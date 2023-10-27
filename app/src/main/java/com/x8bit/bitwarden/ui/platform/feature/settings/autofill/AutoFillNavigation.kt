package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

private const val AUTO_FILL_ROUTE = "settings_auto_fill"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.autoFillDestination(
    onNavigateBack: () -> Unit,
) {
    composable(AUTO_FILL_ROUTE) {
        AutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the auto-fill screen.
 */
fun NavController.navigateToAutoFill(navOptions: NavOptions? = null) {
    navigate(AUTO_FILL_ROUTE, navOptions)
}

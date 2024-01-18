package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val BLOCK_AUTO_FILL_ROUTE = "settings_block_auto_fill"

/**
 * Add block auto-fill destination to the nav graph.
 */
fun NavGraphBuilder.blockAutoFillDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = BLOCK_AUTO_FILL_ROUTE,
    ) {
        BlockAutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the block auto-fill screen.
 */
fun NavController.navigateToBlockAutoFillScreen(navOptions: NavOptions? = null) {
    navigate(BLOCK_AUTO_FILL_ROUTE, navOptions)
}

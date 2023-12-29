package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val OTHER_ROUTE = "settings_other"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.otherDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = OTHER_ROUTE,
    ) {
        OtherScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToOther(navOptions: NavOptions? = null) {
    navigate(OTHER_ROUTE, navOptions)
}

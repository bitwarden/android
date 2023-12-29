package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val NEW_SEND_ROUTE = "new_send"

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.newSendDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = NEW_SEND_ROUTE,
    ) {
        NewSendScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToNewSend(navOptions: NavOptions? = null) {
    navigate(NEW_SEND_ROUTE, navOptions)
}

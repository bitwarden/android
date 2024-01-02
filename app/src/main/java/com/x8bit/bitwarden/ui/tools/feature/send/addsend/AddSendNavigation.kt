package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val ADD_SEND_ROUTE = "add_send"

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.addSendDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ADD_SEND_ROUTE,
    ) {
        AddSendScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToAddSend(navOptions: NavOptions? = null) {
    navigate(ADD_SEND_ROUTE, navOptions)
}

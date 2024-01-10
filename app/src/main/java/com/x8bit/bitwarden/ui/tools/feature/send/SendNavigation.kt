package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithRootPushTransitions

const val SEND_ROUTE: String = "send"

/**
 * Add send destination to the nav graph.
 */
fun NavGraphBuilder.sendDestination(
    onNavigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
) {
    composableWithRootPushTransitions(
        route = SEND_ROUTE,
    ) {
        SendScreen(
            onNavigateToAddSend = onNavigateToAddSend,
            onNavigateToEditSend = onNavigateToEditSend,
        )
    }
}

/**
 * Navigate to the send screen. Note this will only work if send screen was added
 * via [sendDestination].
 */
fun NavController.navigateToSend(navOptions: NavOptions? = null) {
    navigate(SEND_ROUTE, navOptions)
}

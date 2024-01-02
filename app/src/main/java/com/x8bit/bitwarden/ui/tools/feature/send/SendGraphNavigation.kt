package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation

const val SEND_GRAPH_ROUTE: String = "send_graph"

/**
 * Add send destination to the nav graph.
 */
fun NavGraphBuilder.sendGraph(
    onNavigateToAddSend: () -> Unit,
) {
    navigation(
        startDestination = SEND_ROUTE,
        route = SEND_GRAPH_ROUTE,
    ) {
        sendDestination(onNavigateToAddSend = onNavigateToAddSend)
    }
}

/**
 * Navigate to the send screen. Note this will only work if send screen was added
 * via [sendGraph].
 */
fun NavController.navigateToSendGraph(navOptions: NavOptions? = null) {
    navigate(SEND_GRAPH_ROUTE, navOptions)
}

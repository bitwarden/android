package com.x8bit.bitwarden.ui.autofill.fido2

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.base.util.composableWithRootPushTransitions

const val FIDO_2_GRAPH_ROUTE = "fido2_graph"
private const val FIDO_2_ROUTE = "fido2"

/**
 * Navigate to the FIDO nav graph.
 */
fun NavController.navigateToFido2Graph(
    navOptions: NavOptions? = null,
) {
    navigate(FIDO_2_GRAPH_ROUTE, navOptions = navOptions)
}

/**
 * Add the FIDO 2 graph to the nav graph.
 */
fun NavGraphBuilder.fido2Graph() {
    navigation(
        startDestination = FIDO_2_ROUTE,
        route = FIDO_2_GRAPH_ROUTE,
    ) {
        fido2Destination()
    }
}

/**
 * Add the FIDO 2 screen to the nav graph.
 */
fun NavGraphBuilder.fido2Destination() {
    composableWithRootPushTransitions(
        route = FIDO_2_ROUTE,
    ) {
        Fido2Screen()
    }
}

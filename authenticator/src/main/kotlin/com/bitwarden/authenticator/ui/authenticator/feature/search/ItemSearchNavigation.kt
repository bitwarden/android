package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.authenticator.ui.platform.base.util.composableWithSlideTransitions

const val ITEM_SEARCH_ROUTE = "item_search"

/**
 * Add item search destination to the nav graph.
 */
fun NavGraphBuilder.itemSearchDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ITEM_SEARCH_ROUTE,
    ) {
        ItemSearchScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the item search screen.
 */
fun NavController.navigateToSearch() {
    navigate(route = ITEM_SEARCH_ROUTE)
}

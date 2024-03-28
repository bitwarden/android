package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavGraphBuilder
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.composableWithPushTransitions

const val ITEM_LIST_ROUTE = "item_list"

/**
 * Add the item listing screen to the nav graph.
 */
fun NavGraphBuilder.itemListingDestination(
    onNavigateBack: () -> Unit = { },
    onNavigateToAddItemScreen: () -> Unit = { },
    onNavigateToItemScreen: (id: String) -> Unit = { },
    onNavigateToEditItemScreen: (id: String) -> Unit = { },
) {
    composableWithPushTransitions(
        route = ITEM_LIST_ROUTE,
    ) {
        ItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddItemScreen = onNavigateToAddItemScreen,
            onNavigateToItemScreen = onNavigateToItemScreen,
            onNavigateToEditItemScreen = onNavigateToEditItemScreen
        )
    }
}

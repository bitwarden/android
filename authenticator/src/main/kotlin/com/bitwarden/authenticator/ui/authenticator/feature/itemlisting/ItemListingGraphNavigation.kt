package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the item listing graph.
 */
@Serializable
data object ItemListingGraphRoute

/**
 * Add the item listing graph to the nav graph.
 */
fun NavGraphBuilder.itemListingGraph(
    navigateBack: () -> Unit,
    navigateToSearch: () -> Unit,
    navigateToQrCodeScanner: () -> Unit,
    navigateToManualKeyEntry: () -> Unit,
    navigateToEditItem: (String) -> Unit,
) {
    navigation<ItemListingGraphRoute>(
        startDestination = ItemListingRoute,
    ) {
        itemListingDestination(
            onNavigateBack = navigateBack,
            onNavigateToSearch = navigateToSearch,
            onNavigateToQrCodeScanner = navigateToQrCodeScanner,
            onNavigateToManualKeyEntry = navigateToManualKeyEntry,
            onNavigateToEditItemScreen = navigateToEditItem,
        )
    }
}

/**
 * Navigate to the item listing graph.
 */
fun NavController.navigateToItemListGraph(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = ItemListingGraphRoute,
        navOptions = navOptions,
    )
}

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
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
) {
    navigation<ItemListingGraphRoute>(
        startDestination = ItemListingRoute,
    ) {
        itemListingDestination(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItemScreen = onNavigateToEditItem,
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

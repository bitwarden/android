package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.itemDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.navigateToItem
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.manualCodeEntryDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.qrCodeScanDestination

const val ITEM_LISTING_GRAPH_ROUTE = "item_listing_graph"

/**
 * Add the item listing graph to the nav graph.
 */
fun NavGraphBuilder.itemListingGraph(
    navController: NavController,
) {
    navigation(
        route = ITEM_LISTING_GRAPH_ROUTE,
        startDestination = ITEM_LIST_ROUTE
    ) {
        itemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQrCodeScanner = { navController.navigateToQrCodeScanScreen() },
            onNavigateToItemScreen = { navController.navigateToItem(itemId = it) },
            onNavigateToEditItemScreen = { /*navController.navigateToEditItem(itemId = it)*/ },
            onNavigateToManualKeyEntry = { navController.navigateToManualCodeEntryScreen() },
            onNavigateToSyncWithBitwardenScreen = {
                /*navController.navigateToSyncWithBitwardenScreen()*/
            },
            onNavigateToImportScreen = { /*navController.navigateToImportScreen()*/ }
        )
        itemDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEditItem = { /*navController.navigateToEditItem(itemId = it)*/ }
        )
        qrCodeScanDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToManualCodeEntryScreen = {
                navController.popBackStack()
                navController.navigateToManualCodeEntryScreen()
            },
        )
        manualCodeEntryDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQrCodeScreen = {
                navController.popBackStack()
                navController.navigateToQrCodeScanScreen()
            }
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
        route = ITEM_LISTING_GRAPH_ROUTE,
        navOptions = navOptions
    )
}

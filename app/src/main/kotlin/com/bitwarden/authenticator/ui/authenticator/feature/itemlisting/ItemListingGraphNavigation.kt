package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.editItemDestination
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.manualCodeEntryDestination
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.qrCodeScanDestination
import com.bitwarden.authenticator.ui.authenticator.feature.search.itemSearchDestination
import com.bitwarden.authenticator.ui.platform.feature.settings.settingsGraph

const val ITEM_LISTING_GRAPH_ROUTE = "item_listing_graph"

/**
 * Add the item listing graph to the nav graph.
 */
fun NavGraphBuilder.itemListingGraph(
    navController: NavController,
    navigateToSearch: () -> Unit,
    navigateToQrCodeScanner: () -> Unit,
    navigateToManualKeyEntry: () -> Unit,
    navigateToEditItem: (String) -> Unit,
    navigateToTutorial: () -> Unit,
) {
    navigation(
        route = ITEM_LISTING_GRAPH_ROUTE,
        startDestination = ITEM_LIST_ROUTE,
    ) {
        itemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSearch = navigateToSearch,
            onNavigateToQrCodeScanner = navigateToQrCodeScanner,
            onNavigateToManualKeyEntry = navigateToManualKeyEntry,
            onNavigateToEditItemScreen = navigateToEditItem,
        )
        editItemDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        itemSearchDestination(
            onNavigateBack = { navController.popBackStack() }
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
        settingsGraph(
            navController = navController,
            onNavigateToTutorial = navigateToTutorial
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

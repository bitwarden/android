package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.authenticator

import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.editItemDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.manualCodeEntryDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.navbar.AUTHENTICATOR_NAV_BAR_ROUTE
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.navbar.authenticatorNavBarDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.qrCodeScanDestination
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search.navigateToSearch

const val AUTHENTICATOR_GRAPH_ROUTE = "authenticator_graph"

/**
 * Navigate to the authenticator graph
 */
fun NavController.navigateToAuthenticatorGraph(navOptions: NavOptions? = null) {
    navigate(AUTHENTICATOR_NAV_BAR_ROUTE, navOptions)
}

/**
 * Add the top authenticator graph to the nav graph.
 */
fun NavGraphBuilder.authenticatorGraph(
    navController: NavController,
) {
    navigation(
        startDestination = AUTHENTICATOR_NAV_BAR_ROUTE,
        route = AUTHENTICATOR_GRAPH_ROUTE
    ) {
        authenticatorNavBarDestination(
            onNavigateToSearch = { navController.navigateToSearch() },
            onNavigateToQrCodeScanner = { navController.navigateToQrCodeScanScreen() },
            onNavigateToManualKeyEntry = { navController.navigateToManualCodeEntryScreen() },
            onNavigateToEditItem = { navController.navigateToEditItem(itemId = it) },
        )
        itemListingGraph(
            navController = navController,
            navigateToSearch = {
                navController.navigateToSearch()
            },
            navigateToQrCodeScanner = {
                navController.navigateToQrCodeScanScreen()
            },
            navigateToManualKeyEntry = {
                navController.navigateToManualCodeEntryScreen()
            },
            navigateToEditItem = {
                navController.navigateToEditItem(itemId = it)
            }
        )
        itemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQrCodeScanner = { navController.navigateToQrCodeScanScreen() },
            onNavigateToManualKeyEntry = { navController.navigateToManualCodeEntryScreen() },
            onNavigateToEditItemScreen = { navController.navigateToEditItem(itemId = it) },
            onNavigateToSyncWithBitwardenScreen = {
                Toast
                    .makeText(
                        navController.context,
                        R.string.not_yet_implemented,
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                /*navController.navigateToSyncWithBitwardenScreen()*/
            },
            onNavigateToImportScreen = {
                Toast
                    .makeText(
                        navController.context,
                        R.string.not_yet_implemented,
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                /*navController.navigateToImportScreen()*/
            },
            onNavigateToSearch = { navController.navigateToSearch() },
        )
        editItemDestination(
            onNavigateBack = { navController.popBackStack() },
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

package com.bitwarden.authenticator.ui.authenticator.feature.authenticator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.AUTHENTICATOR_NAV_BAR_ROUTE
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.authenticatorNavBarDestination
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.bitwarden.authenticator.ui.authenticator.feature.search.navigateToSearch
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToTutorial

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
            onNavigateToTutorial = { navController.navigateToTutorial() },
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
            },
            navigateToTutorial = { navController.navigateToTutorial() },
        )
    }
}

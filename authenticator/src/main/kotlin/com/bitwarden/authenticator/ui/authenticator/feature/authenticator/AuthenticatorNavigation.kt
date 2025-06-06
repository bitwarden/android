package com.bitwarden.authenticator.ui.authenticator.feature.authenticator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.AuthenticatorNavbarRoute
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.authenticatorNavBarDestination
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.bitwarden.authenticator.ui.authenticator.feature.search.navigateToSearch
import com.bitwarden.authenticator.ui.platform.feature.settings.export.navigateToExport
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.navigateToImporting
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToSettingsTutorial
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the authenticator graph.
 */
@Serializable
data object AuthenticatorGraphRoute

/**
 * Navigate to the authenticator graph
 */
fun NavController.navigateToAuthenticatorGraph(navOptions: NavOptions? = null) {
    navigate(route = AuthenticatorNavbarRoute, navOptions = navOptions)
}

/**
 * Add the top authenticator graph to the nav graph.
 */
fun NavGraphBuilder.authenticatorGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    navigation<AuthenticatorGraphRoute>(
        startDestination = AuthenticatorNavbarRoute,
    ) {
        authenticatorNavBarDestination(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = { navController.navigateToSearch() },
            onNavigateToQrCodeScanner = { navController.navigateToQrCodeScanScreen() },
            onNavigateToManualKeyEntry = { navController.navigateToManualCodeEntryScreen() },
            onNavigateToEditItem = { navController.navigateToEditItem(itemId = it) },
            onNavigateToExport = { navController.navigateToExport() },
            onNavigateToImport = { navController.navigateToImporting() },
            onNavigateToTutorial = { navController.navigateToSettingsTutorial() },
        )
        itemListingGraph(
            navController = navController,
            navigateBack = onNavigateBack,
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
            navigateToExport = { navController.navigateToExport() },
            navigateToImport = { navController.navigateToImporting() },
            navigateToTutorial = { navController.navigateToSettingsTutorial() },
        )
    }
}

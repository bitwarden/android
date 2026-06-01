@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.authenticator.feature.authenticator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.editItemDestination
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.manualCodeEntryDestination
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.AuthenticatorNavbarRoute
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.authenticatorNavBarDestination
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.qrCodeScanDestination
import com.bitwarden.authenticator.ui.authenticator.feature.search.itemSearchDestination
import com.bitwarden.authenticator.ui.authenticator.feature.search.navigateToSearch
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToSettingsTutorial
import com.bitwarden.authenticator.ui.platform.feature.tutorial.tutorialSettingsDestination
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
) {
    navigation<AuthenticatorGraphRoute>(
        startDestination = AuthenticatorNavbarRoute,
    ) {
        authenticatorNavBarDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSearch = { navController.navigateToSearch() },
            onNavigateToQrCodeScanner = { navController.navigateToQrCodeScanScreen() },
            onNavigateToManualKeyEntry = { navController.navigateToManualCodeEntryScreen() },
            onNavigateToEditItem = { navController.navigateToEditItem(itemId = it) },
            onNavigateToTutorial = { navController.navigateToSettingsTutorial() },
        )
        editItemDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        itemSearchDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEdit = { navController.navigateToEditItem(itemId = it) },
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
            },
        )
        tutorialSettingsDestination(
            onTutorialFinished = { navController.popBackStack() },
        )
    }
}

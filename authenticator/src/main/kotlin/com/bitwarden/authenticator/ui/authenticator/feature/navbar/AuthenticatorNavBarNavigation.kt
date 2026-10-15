@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.AuthenticatorGraph
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingGraph
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingDestination
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.navigateToManualCodeEntryScreen
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.navigateToQrCodeScanScreen
import com.bitwarden.authenticator.ui.authenticator.feature.search.navigateToSearch
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraph
import com.bitwarden.authenticator.ui.platform.feature.settings.export.exportDestination
import com.bitwarden.authenticator.ui.platform.feature.settings.export.navigateToExport
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.importingDestination
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.navigateToImporting
import com.bitwarden.authenticator.ui.platform.feature.settings.settingsDestination
import com.bitwarden.authenticator.ui.platform.feature.tutorial.navigateToSettingsTutorial
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenTabNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavKeyEntries
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.stayEntry

/**
 * Add the authenticator nav bar destination to the authenticator navigation entries. The
 * [AuthenticatorGraph.NavBarRoute] hosts the [AuthenticatorNavBarGraph], where each tab has its
 * own back stack within its own navigation graph.
 */
fun EntryProviderScope<AuthenticatorGraph>.authenticatorNavBarDestination(
    navigationController: NavigationController<AuthenticatorGraph>,
) {
    stayEntry<AuthenticatorGraph.NavBarRoute> {
        AuthenticatorNavBarNavDisplay(authenticatorNavigationController = navigationController)
    }
}

@Composable
private fun AuthenticatorNavBarNavDisplay(
    authenticatorNavigationController: NavigationController<AuthenticatorGraph>,
) {
    val navigationController = rememberNavigationController<AuthenticatorNavBarGraph>(
        routes = arrayOf(AuthenticatorNavBarGraph.VerificationCodes),
    )
    val tabNavigationControllers = rememberAuthenticatorNavBarNavigationControllers(
        navBarNavigationController = navigationController,
    )
    AuthenticatorNavBarScreen(bottomNavigationController = tabNavigationControllers) {
        val tabEntries = tabNavigationControllers.rememberTabNavEntries(
            authenticatorNavigationController = authenticatorNavigationController,
        )
        BitwardenTabNavDisplay(
            bottomNavigationController = tabNavigationControllers,
            tabEntries = tabEntries,
        )
    }
}

@Composable
private fun AuthenticatorNavBarNavigationControllers.rememberTabNavEntries(
    authenticatorNavigationController: NavigationController<AuthenticatorGraph>,
): Map<AuthenticatorNavBarGraph, List<NavEntry<NavKey>>> = mapOf(
    AuthenticatorNavBarGraph.VerificationCodes to itemListing.rememberNavKeyEntries(
        entryProvider = itemListingEntryProvider(
            authenticatorNavigationController = authenticatorNavigationController,
        ),
    ),
    AuthenticatorNavBarGraph.Settings to settings.rememberNavKeyEntries(
        entryProvider = settingsEntryProvider(
            authenticatorNavigationController = authenticatorNavigationController,
        ),
    ),
)

private fun AuthenticatorNavBarNavigationControllers.itemListingEntryProvider(
    authenticatorNavigationController: NavigationController<AuthenticatorGraph>,
): (key: ItemListingGraph) -> NavEntry<ItemListingGraph> = entryProvider {
    itemListingDestination(
        onNavigateBack = { itemListing.navigateBack() },
        onNavigateToSearch = { authenticatorNavigationController.navigateToSearch() },
        onNavigateToQrCodeScanner = {
            authenticatorNavigationController.navigateToQrCodeScanScreen()
        },
        onNavigateToManualKeyEntry = {
            authenticatorNavigationController.navigateToManualCodeEntryScreen()
        },
        onNavigateToEditItemScreen = {
            authenticatorNavigationController.navigateToEditItem(itemId = it)
        },
    )
}

private fun AuthenticatorNavBarNavigationControllers.settingsEntryProvider(
    authenticatorNavigationController: NavigationController<AuthenticatorGraph>,
): (key: SettingsGraph) -> NavEntry<SettingsGraph> = entryProvider {
    settingsDestination(
        onNavigateToTutorial = { authenticatorNavigationController.navigateToSettingsTutorial() },
        onNavigateToExport = { settings.navigateToExport() },
        onNavigateToImport = { settings.navigateToImporting() },
    )
    exportDestination(onNavigateBack = { settings.navigateBack() })
    importingDestination(onNavigateBack = { settings.navigateBack() })
}

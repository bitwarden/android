@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.authenticator.feature.authenticator

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.editItemDestination
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.navigateToEditItem
import com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry.manualCodeEntryDestination
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.authenticatorNavBarDestination
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.qrCodeScanDestination
import com.bitwarden.authenticator.ui.authenticator.feature.search.itemSearchDestination
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.authenticator.ui.platform.feature.tutorial.tutorialSettingsDestination
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController

/**
 * Add the authenticator section to the root navigation entries. The
 * [RootNavGraph.AuthenticatorRoute] hosts the [AuthenticatorGraph].
 *
 * @param onBackStackUpdate Invoked whenever the back stack of the [AuthenticatorGraph] changes.
 */
fun EntryProviderScope<RootNavGraph>.authenticatorGraph(
    onBackStackUpdate: () -> Unit,
) {
    entry<RootNavGraph.AuthenticatorRoute> {
        val navigationController = rememberNavigationController<AuthenticatorGraph>(
            routes = arrayOf(AuthenticatorGraph.NavBarRoute),
        )
        LaunchedEffect(key1 = navigationController) {
            navigationController.backStackSnapshot.collect { onBackStackUpdate() }
        }
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                authenticatorNavBarDestination(navigationController = navigationController)
                editItemDestination(onNavigateBack = { navigationController.navigateBack() })
                itemSearchDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                    onNavigateToEdit = { navigationController.navigateToEditItem(itemId = it) },
                )
                qrCodeScanDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                    onNavigateToManualCodeEntryScreen = {
                        navigationController.replaceLast(
                            route = AuthenticatorGraph.ManualCodeEntryRoute,
                        )
                    },
                )
                manualCodeEntryDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                    onNavigateToQrCodeScreen = {
                        navigationController.replaceLast(route = AuthenticatorGraph.QrCodeScanRoute)
                    },
                )
                tutorialSettingsDestination(
                    onTutorialFinished = { navigationController.navigateBack() },
                )
            },
        )
    }
}

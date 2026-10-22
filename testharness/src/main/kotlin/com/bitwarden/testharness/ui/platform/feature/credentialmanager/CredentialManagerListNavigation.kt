@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.testharness.ui.platform.feature.createpasskey.createPasskeyDestination
import com.bitwarden.testharness.ui.platform.feature.createpasskey.navigateToCreatePasskey
import com.bitwarden.testharness.ui.platform.feature.createpassword.createPasswordDestination
import com.bitwarden.testharness.ui.platform.feature.createpassword.navigateToCreatePassword
import com.bitwarden.testharness.ui.platform.feature.getpasskey.getPasskeyDestination
import com.bitwarden.testharness.ui.platform.feature.getpasskey.navigateToGetPasskey
import com.bitwarden.testharness.ui.platform.feature.getpassword.getPasswordDestination
import com.bitwarden.testharness.ui.platform.feature.getpassword.navigateToGetPassword
import com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey.getPasswordOrPasskeyDestination
import com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey.navigateToGetPasswordOrPasskey
import com.bitwarden.testharness.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootPushEntry

/**
 * Add the Credential Manager test flow to the root navigation entries. The
 * [RootNavGraph.CredentialManagerRoute] hosts the [CredentialManagerGraph].
 */
fun EntryProviderScope<RootNavGraph>.credentialManagerGraph(
    onNavigateBack: () -> Unit,
) {
    entry<RootNavGraph.CredentialManagerRoute> {
        val navigationController = rememberNavigationController<CredentialManagerGraph>(
            routes = arrayOf(CredentialManagerGraph.CredentialManagerListRoute),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                credentialManagerDestinations(
                    navigationController = navigationController,
                    onNavigateBack = onNavigateBack,
                )
            },
        )
    }
}

/**
 * Add all the Credential Manager test destinations to the Credential Manager navigation entries.
 *
 * The [CredentialManagerGraph.CredentialManagerListRoute] is the root of the Credential Manager
 * test flow, which contains the list screen and all nested credential manager API test screens.
 */
private fun EntryProviderScope<CredentialManagerGraph>.credentialManagerDestinations(
    navigationController: NavigationController<CredentialManagerGraph>,
    onNavigateBack: () -> Unit,
) {
    credentialManagerListDestination(
        onNavigateBack = onNavigateBack,
        onNavigateToGetPassword = { navigationController.navigateToGetPassword() },
        onNavigateToCreatePassword = { navigationController.navigateToCreatePassword() },
        onNavigateToGetPasskey = { navigationController.navigateToGetPasskey() },
        onNavigateToCreatePasskey = { navigationController.navigateToCreatePasskey() },
        onNavigateToGetPasswordOrPasskey = {
            navigationController.navigateToGetPasswordOrPasskey()
        },
    )

    getPasswordDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )

    createPasswordDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )

    getPasskeyDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )

    createPasskeyDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )

    getPasswordOrPasskeyDestination(
        onNavigateBack = { navigationController.navigateBack() },
    )
}

@Suppress("LongParameterList")
private fun EntryProviderScope<CredentialManagerGraph>.credentialManagerListDestination(
    onNavigateBack: () -> Unit,
    onNavigateToGetPassword: () -> Unit,
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToGetPasskey: () -> Unit,
    onNavigateToCreatePasskey: () -> Unit,
    onNavigateToGetPasswordOrPasskey: () -> Unit,
) {
    rootPushEntry<CredentialManagerGraph.CredentialManagerListRoute> {
        CredentialManagerListScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToGetPassword = { onNavigateToGetPassword() },
            onNavigateToCreatePassword = { onNavigateToCreatePassword() },
            onNavigateToGetPasskey = { onNavigateToGetPasskey() },
            onNavigateToCreatePasskey = { onNavigateToCreatePasskey() },
            onNavigateToGetPasswordOrPasskey = { onNavigateToGetPasswordOrPasskey() },
        )
    }
}

/**
 * Navigate to the Credential Manager test flow.
 */
fun NavigationController<RootNavGraph>.navigateToCredentialManager() {
    this.navigate(route = RootNavGraph.CredentialManagerRoute)
}

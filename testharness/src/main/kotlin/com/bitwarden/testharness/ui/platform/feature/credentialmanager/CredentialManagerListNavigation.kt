@file:OmitFromCoverage

package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
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
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import kotlinx.serialization.Serializable

/**
 * Credential Manager graph route - serves as the parent for all Credential Manager API test flows.
 */
@Serializable
data object CredentialManagerGraphRoute

/**
 * Credential Manager test category list screen - the start destination of the graph.
 */
@Serializable
data object CredentialManagerListRoute

/**
 * Add Credential Manager nav graph to the root nav graph.
 *
 * This graph contains the list screen and all nested credential manager API test screens.
 */
fun NavGraphBuilder.credentialManagerGraph(
    onNavigateBack: () -> Unit,
    navController: NavController,
) {
    navigation<CredentialManagerGraphRoute>(
        startDestination = CredentialManagerListRoute,
    ) {
        credentialManagerListDestination(
            onNavigateBack = onNavigateBack,
            onNavigateToGetPassword = {
                navController.navigateToGetPassword()
            },
            onNavigateToCreatePassword = {
                navController.navigateToCreatePassword()
            },
            onNavigateToGetPasskey = {
                navController.navigateToGetPasskey()
            },
            onNavigateToCreatePasskey = {
                navController.navigateToCreatePasskey()
            },
            onNavigateToGetPasswordOrPasskey = {
                navController.navigateToGetPasswordOrPasskey()
            },
        )

        getPasswordDestination(
            onNavigateBack = { navController.popBackStack() },
        )

        createPasswordDestination(
            onNavigateBack = { navController.popBackStack() },
        )

        getPasskeyDestination(
            onNavigateBack = { navController.popBackStack() },
        )

        createPasskeyDestination(
            onNavigateBack = { navController.popBackStack() },
        )

        getPasswordOrPasskeyDestination(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

@Suppress("LongParameterList")
private fun NavGraphBuilder.credentialManagerListDestination(
    onNavigateBack: () -> Unit,
    onNavigateToGetPassword: () -> Unit,
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToGetPasskey: () -> Unit,
    onNavigateToCreatePasskey: () -> Unit,
    onNavigateToGetPasswordOrPasskey: () -> Unit,
) {
    composableWithRootPushTransitions<CredentialManagerListRoute> {
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
 * Navigate to the Credential Manager flow.
 */
fun NavController.navigateToCredentialManagerGraph(navOptions: NavOptions? = null) {
    navigate(route = CredentialManagerGraphRoute, navOptions = navOptions)
}

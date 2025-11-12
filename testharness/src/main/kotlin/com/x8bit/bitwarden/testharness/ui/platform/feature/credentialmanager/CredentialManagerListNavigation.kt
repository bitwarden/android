@file:OmitFromCoverage

package com.x8bit.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.testharness.ui.platform.feature.createpasskey.createPasskeyDestination
import com.x8bit.bitwarden.testharness.ui.platform.feature.createpasskey.navigateToCreatePasskey
import com.x8bit.bitwarden.testharness.ui.platform.feature.createpassword.createPasswordDestination
import com.x8bit.bitwarden.testharness.ui.platform.feature.createpassword.navigateToCreatePassword
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpasskey.getPasskeyDestination
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpasskey.navigateToGetPasskey
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpassword.getPasswordDestination
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpassword.navigateToGetPassword
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey.getPasswordOrPasskeyDestination
import com.x8bit.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey.navigateToGetPasswordOrPasskey
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
        composable<CredentialManagerListRoute> {
            CredentialManagerListScreen(
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
        }

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

/**
 * Navigate to the Credential Manager flow.
 */
fun NavController.navigateToCredentialManagerGraph(navOptions: NavOptions? = null) {
    navigate(route = CredentialManagerGraphRoute, navOptions = navOptions)
}

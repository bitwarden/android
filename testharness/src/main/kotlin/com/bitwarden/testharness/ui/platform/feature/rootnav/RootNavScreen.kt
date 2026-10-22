package com.bitwarden.testharness.ui.platform.feature.rootnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bitwarden.testharness.ui.platform.feature.autofill.autofillGraph
import com.bitwarden.testharness.ui.platform.feature.autofill.navigateToAutofill
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.credentialManagerGraph
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.navigateToCredentialManager
import com.bitwarden.testharness.ui.platform.feature.landing.landingDestination
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController

/**
 * Controls the root level [NavDisplay] for the test harness app. Each route in the [RootNavGraph]
 * represents a top-level section of the test harness, which may host its own nested navigation
 * graph.
 */
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navigationController: NavigationController<RootNavGraph> = rememberNavigationController(
        routes = arrayOf(RootNavGraph.LandingRoute),
    ),
    entryProvider: (RootNavGraph) -> NavEntry<RootNavGraph> = entryProvider(
        navigationController = navigationController,
    ),
    onSplashScreenRemoved: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state != RootNavState.Splash) {
            onSplashScreenRemoved()
        }
    }

    BitwardenNavDisplay(
        navigationController = navigationController,
        entryProvider = entryProvider,
    )
}

private fun entryProvider(
    navigationController: NavigationController<RootNavGraph>,
): (RootNavGraph) -> NavEntry<RootNavGraph> = entryProvider {
    landingDestination(
        onNavigateToAutofill = { navigationController.navigateToAutofill() },
        onNavigateToCredentialManager = { navigationController.navigateToCredentialManager() },
    )
    autofillGraph(
        onNavigateBack = { navigationController.navigateBack() },
    )
    credentialManagerGraph(
        onNavigateBack = { navigationController.navigateBack() },
    )
}

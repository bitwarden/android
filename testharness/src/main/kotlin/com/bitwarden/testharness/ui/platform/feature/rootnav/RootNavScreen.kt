package com.bitwarden.testharness.ui.platform.feature.rootnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bitwarden.testharness.ui.platform.feature.autofill.autofillGraph
import com.bitwarden.testharness.ui.platform.feature.autofill.navigateToAutofillGraph
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.credentialManagerGraph
import com.bitwarden.testharness.ui.platform.feature.credentialmanager.navigateToCredentialManagerGraph
import com.bitwarden.testharness.ui.platform.feature.landing.LandingRoute
import com.bitwarden.testharness.ui.platform.feature.landing.landingDestination

/**
 * Controls the root level [NavHost] for the test harness app.
 */
@Suppress("LongMethod")
@Composable
fun RootNavScreen(
    viewModel: RootNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onSplashScreenRemoved: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state != RootNavState.Splash) {
            onSplashScreenRemoved()
        }
    }

    NavHost(
        navController = navController,
        startDestination = LandingRoute,
    ) {
        landingDestination(
            onNavigateToAutofill = {
                navController.navigateToAutofillGraph()
            },
            onNavigateToCredentialManager = {
                navController.navigateToCredentialManagerGraph()
            },
        )

        autofillGraph(
            onNavigateBack = { navController.popBackStack() },
        )

        credentialManagerGraph(
            onNavigateBack = { navController.popBackStack() },
            navController = navController,
        )
    }
}

package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.util.rememberBitwardenNavController
import com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure.accessibilityDisclosureDestination
import com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure.navigateToAccessibilityDisclosure
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.cookieAcquisitionDestination
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.navigateToCookieAcquisition
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.localNetworkAccessDestination
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.navigateToLocalNetworkAccess
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavigationRoute
import com.x8bit.bitwarden.ui.platform.feature.rootnav.rootNavDestination

/**
 * Controls the overlay [NavHost] for the app including the [rootNavDestination] and any screen
 * that can appear on top of it without affecting its state.
 */
@Composable
fun OverlayNavScreen(
    viewModel: OverlayNavViewModel = hiltViewModel(),
    navController: NavHostController = rememberBitwardenNavController(name = "OverlayNavScreen"),
    onSplashScreenRemoved: () -> Unit,
) {
    OverlayNavEventsEffect(
        viewModel = viewModel,
        navController = navController,
    )
    NavHost(
        navController = navController,
        startDestination = RootNavigationRoute,
    ) {
        // This is the overlay level of navigation that sits above the root nav. These screens
        // can appear on top of the rest of the app without interacting with the state-based
        // navigation used by RootNavScreen (which also exists here).
        rootNavDestination(onSplashScreenRemoved = onSplashScreenRemoved)
        cookieAcquisitionDestination(
            onDismiss = { navController.popBackStack() },
            onSplashScreenRemoved = onSplashScreenRemoved,
        )
        localNetworkAccessDestination(
            onDismiss = { navController.popBackStack() },
            onSplashScreenRemoved = onSplashScreenRemoved,
        )
        accessibilityDisclosureDestination(
            onDismiss = { navController.popBackStack() },
            onSplashScreenRemoved = onSplashScreenRemoved,
        )
    }
}

@Composable
private fun OverlayNavEventsEffect(
    viewModel: OverlayNavViewModel,
    navController: NavController,
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            OverlayNavEvent.NavigateToCookieAcquisition -> {
                navController.navigateToCookieAcquisition()
            }

            OverlayNavEvent.NavigateToLocalNetworkAccess -> {
                navController.navigateToLocalNetworkAccess()
            }

            OverlayNavEvent.NavigateToAccessibilityDisclosure -> {
                navController.navigateToAccessibilityDisclosure()
            }
        }
    }
}

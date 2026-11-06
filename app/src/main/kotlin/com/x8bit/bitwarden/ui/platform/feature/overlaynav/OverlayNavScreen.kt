package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure.accessibilityDisclosureDestination
import com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure.navigateToAccessibilityDisclosure
import com.x8bit.bitwarden.ui.platform.feature.biometrics.navigateToUpdateBiometrics
import com.x8bit.bitwarden.ui.platform.feature.biometrics.updateBiometricsDestination
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.cookieAcquisitionDestination
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.navigateToCookieAcquisition
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.localNetworkAccessDestination
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.navigateToLocalNetworkAccess
import com.x8bit.bitwarden.ui.platform.feature.rootnav.rootNavDestination

/**
 * Controls the overlay [NavDisplay] for the app including the [rootNavDestination] and any screen
 * that can appear on top of it without affecting its state.
 */
@Composable
fun OverlayNavScreen(
    viewModel: OverlayNavViewModel = hiltViewModel(),
    navigationController: NavigationController<OverlayNavGraph> = rememberNavigationController(
        routes = arrayOf(OverlayNavGraph.RootNavigationRoute),
    ),
    onSplashScreenRemoved: () -> Unit,
    entryProvider: (OverlayNavGraph) -> NavEntry<OverlayNavGraph> = entryProvider(
        navigationController = navigationController,
        onSplashScreenRemoved = onSplashScreenRemoved,
    ),
) {
    OverlayNavEventsEffect(
        viewModel = viewModel,
        navigationController = navigationController,
    )
    BitwardenNavDisplay(
        navigationController = navigationController,
        entryProvider = entryProvider,
    )
}

private fun entryProvider(
    navigationController: NavigationController<OverlayNavGraph>,
    onSplashScreenRemoved: () -> Unit,
): (OverlayNavGraph) -> NavEntry<OverlayNavGraph> = entryProvider {
    // This is the overlay level of navigation that sits above the root nav. These screens
    // can appear on top of the rest of the app without interacting with the state-based
    // navigation used by RootNavScreen (which also exists here).
    rootNavDestination(
        onSplashScreenRemoved = onSplashScreenRemoved,
    )
    cookieAcquisitionDestination(
        onDismiss = { navigationController.navigateBack() },
        onSplashScreenRemoved = onSplashScreenRemoved,
    )
    localNetworkAccessDestination(
        onDismiss = { navigationController.navigateBack() },
        onSplashScreenRemoved = onSplashScreenRemoved,
    )
    accessibilityDisclosureDestination(
        onDismiss = { navigationController.navigateBack() },
        onSplashScreenRemoved = onSplashScreenRemoved,
    )
    updateBiometricsDestination(
        onDismiss = { navigationController.navigateBack() },
        onSplashScreenRemoved = onSplashScreenRemoved,
    )
}

@Composable
private fun OverlayNavEventsEffect(
    viewModel: OverlayNavViewModel,
    navigationController: NavigationController<OverlayNavGraph>,
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            OverlayNavEvent.NavigateToUpdateBiometrics -> {
                navigationController.navigateToUpdateBiometrics()
            }

            OverlayNavEvent.NavigateToCookieAcquisition -> {
                navigationController.navigateToCookieAcquisition()
            }

            OverlayNavEvent.NavigateToLocalNetworkAccess -> {
                navigationController.navigateToLocalNetworkAccess()
            }

            OverlayNavEvent.NavigateToAccessibilityDisclosure -> {
                navigationController.navigateToAccessibilityDisclosure()
            }
        }
    }
}

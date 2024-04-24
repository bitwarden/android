package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.navigation.NavGraphBuilder
import com.bitwarden.authenticator.ui.platform.base.util.composableWithStayTransitions

const val AUTHENTICATOR_NAV_BAR_ROUTE: String = "AuthenticatorNavBarRoute"

/**
 * Add the authenticator nav bar to the nav graph.
 */
fun NavGraphBuilder.authenticatorNavBarDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItem: (itemId: String) -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToExport: () -> Unit,
) {
    composableWithStayTransitions(
        route = AUTHENTICATOR_NAV_BAR_ROUTE,
    ) {
        AuthenticatorNavBarScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItem = onNavigateToEditItem,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToTutorial = onNavigateToTutorial,
            onNavigateToExport = onNavigateToExport,
        )
    }
}

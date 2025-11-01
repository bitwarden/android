package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.navigation.NavGraphBuilder
import com.bitwarden.ui.platform.base.util.composableWithStayTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the authenticator navbar screen.
 */
@Serializable
data object AuthenticatorNavbarRoute

/**
 * Add the authenticator nav bar to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.authenticatorNavBarDestination(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItem: (itemId: String) -> Unit,
    onNavigateToTutorial: () -> Unit,
) {
    composableWithStayTransitions<AuthenticatorNavbarRoute> {
        AuthenticatorNavBarScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItem = onNavigateToEditItem,
            onNavigateToTutorial = onNavigateToTutorial,
        )
    }
}

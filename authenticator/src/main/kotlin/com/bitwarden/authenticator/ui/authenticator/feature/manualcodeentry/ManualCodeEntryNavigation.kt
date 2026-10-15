package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.AuthenticatorGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry

/**
 * Add the manual code entry screen to the navigation entries.
 */
fun EntryProviderScope<AuthenticatorGraph>.manualCodeEntryDestination(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScreen: () -> Unit,
) {
    slideUpEntry<AuthenticatorGraph.ManualCodeEntryRoute> {
        ManualCodeEntryScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToQrCodeScreen = onNavigateToQrCodeScreen,
        )
    }
}

/**
 * Navigate to the manual code entry screen.
 */
fun NavigationController<AuthenticatorGraph>.navigateToManualCodeEntryScreen() {
    this.navigate(route = AuthenticatorGraph.ManualCodeEntryRoute)
}

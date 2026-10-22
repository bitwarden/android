package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the master password guidance screen.
 */
fun NavigationController<AuthGraph>.navigateToMasterPasswordGuidance() {
    this.navigate(route = AuthGraph.MasterPasswordGuidanceRoute)
}

/**
 * Add the master password guidance screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.masterPasswordGuidanceDestination(
    onNavigateBack: () -> Unit,
    onNavigateToGeneratePassword: () -> Unit,
) {
    slideUpEntry<AuthGraph.MasterPasswordGuidanceRoute> {
        MasterPasswordGuidanceScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToGeneratePassword = onNavigateToGeneratePassword,
        )
    }
}

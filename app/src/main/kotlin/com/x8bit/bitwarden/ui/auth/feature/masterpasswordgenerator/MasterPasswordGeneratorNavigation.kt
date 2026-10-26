package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to master password generator  screen.
 */
fun NavigationController<AuthGraph>.navigateToMasterPasswordGenerator() {
    this.navigate(route = AuthGraph.MasterPasswordGeneratorRoute)
}

/**
 * Add the master password generator screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.masterPasswordGeneratorDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPreventLockout: () -> Unit,
    onNavigateBackWithPassword: () -> Unit,
) {
    slideUpEntry<AuthGraph.MasterPasswordGeneratorRoute> {
        MasterPasswordGeneratorScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPreventLockout = onNavigateToPreventLockout,
            onNavigateBackWithPassword = onNavigateBackWithPassword,
        )
    }
}

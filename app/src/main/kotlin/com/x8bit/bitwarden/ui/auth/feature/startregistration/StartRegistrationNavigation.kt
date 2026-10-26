package com.x8bit.bitwarden.ui.auth.feature.startregistration

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Navigate to the start registration screen.
 */
fun NavigationController<AuthGraph>.navigateToStartRegistration() {
    this.navigate(route = AuthGraph.StartRegistrationRoute)
}

/**
 * Add the start registration screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.startRegistrationDestination(
    onNavigateBack: () -> Unit,
    onNavigateToCompleteRegistration: (emailAddress: String, verificationToken: String) -> Unit,
    onNavigateToCheckEmail: (email: String) -> Unit,
    onNavigateToEnvironment: () -> Unit,
) {
    slideUpEntry<AuthGraph.StartRegistrationRoute> {
        StartRegistrationScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToCompleteRegistration = onNavigateToCompleteRegistration,
            onNavigateToCheckEmail = onNavigateToCheckEmail,
            onNavigateToEnvironment = onNavigateToEnvironment,
        )
    }
}

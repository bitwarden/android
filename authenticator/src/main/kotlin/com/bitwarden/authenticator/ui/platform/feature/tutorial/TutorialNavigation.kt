package com.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.authenticator.ui.authenticator.feature.authenticator.AuthenticatorGraph
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavGraph
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.bitwarden.ui.platform.navigation.util.stayEntry

/**
 * Add the top level Tutorial screen to the root navigation entries.
 */
fun EntryProviderScope<RootNavGraph>.tutorialDestination() {
    stayEntry<RootNavGraph.TutorialRoute> {
        TutorialScreen(
            onTutorialFinished = {
                // State-based navigation will handle this.
            },
        )
    }
}

/**
 * Add the Settings Tutorial screen to the authenticator navigation entries.
 */
fun EntryProviderScope<AuthenticatorGraph>.tutorialSettingsDestination(
    onTutorialFinished: () -> Unit,
) {
    slideUpEntry<AuthenticatorGraph.SettingsTutorialRoute> {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

/**
 * Navigate to the Tutorial screen within Settings.
 */
fun NavigationController<AuthenticatorGraph>.navigateToSettingsTutorial() {
    this.navigate(route = AuthenticatorGraph.SettingsTutorialRoute)
}

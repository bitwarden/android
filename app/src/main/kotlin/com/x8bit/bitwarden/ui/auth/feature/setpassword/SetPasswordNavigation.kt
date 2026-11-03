package com.x8bit.bitwarden.ui.auth.feature.setpassword

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Add the Set Password screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.setPasswordDestination() {
    entry<AuthGraph.SetPasswordRoute> {
        SetPasswordScreen()
    }
}

/**
 * Navigate to the Set Password screen.
 */
fun NavigationController<AuthGraph>.navigateToSetPassword() {
    this.navigate(route = AuthGraph.SetPasswordRoute)
}

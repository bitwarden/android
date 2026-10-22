package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.navigation3.runtime.EntryProviderScope

/**
 * Add the setup complete screen to the account setup navigation entries.
 */
fun EntryProviderScope<AccountSetupGraph>.setupCompleteDestination() {
    entry<AccountSetupGraph.SetupCompleteRoute> {
        SetupCompleteScreen()
    }
}

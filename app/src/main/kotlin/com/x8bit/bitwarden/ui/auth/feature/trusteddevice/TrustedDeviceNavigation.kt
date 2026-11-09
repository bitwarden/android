package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph

/**
 * Add the Trusted Device Screen to the navigation entries.
 */
fun EntryProviderScope<AuthGraph>.trustedDeviceDestination(
    onNavigateToAdminApproval: (emailAddress: String) -> Unit,
    onNavigateToLoginWithOtherDevice: (emailAddress: String) -> Unit,
    onNavigateToLock: (emailAddress: String) -> Unit,
) {
    slideUpEntry<AuthGraph.TrustedDeviceRoute> {
        TrustedDeviceScreen(
            onNavigateToAdminApproval = onNavigateToAdminApproval,
            onNavigateToLoginWithOtherDevice = onNavigateToLoginWithOtherDevice,
            onNavigateToLock = onNavigateToLock,
        )
    }
}

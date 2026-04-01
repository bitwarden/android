package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the manage devices screen.
 */
@Serializable
data object ManageDevicesRoute

/**
 * Add manage devices destinations to the nav graph.
 */
fun NavGraphBuilder.manageDevicesDestination(
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprintPhrase: String) -> Unit,
) {
    composableWithSlideTransitions<ManageDevicesRoute> {
        ManageDevicesScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToLoginApproval = onNavigateToLoginApproval,
        )
    }
}

/**
 * Navigate to the Manage Devices screen.
 */
fun NavController.navigateToManageDevices(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = ManageDevicesRoute, navOptions = navOptions)
}

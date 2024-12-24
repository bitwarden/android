package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val NEW_DEVICE_NOTICE_TWO_FACTOR_ROUTE = "new_device_notice_two_factor"

/**
 * Navigate to the new device notice two factor screen.
 */
fun NavController.navigateToNewDeviceNoticeTwoFactor(
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = NEW_DEVICE_NOTICE_TWO_FACTOR_ROUTE,
        navOptions = navOptions,
    )
}

/**
 * Add the new device notice two factor screen to the nav graph.
 */
fun NavGraphBuilder.newDeviceNoticeTwoFactorDestination(
    onNavigateBackToVault: () -> Unit,
) {
    composableWithSlideTransitions(
        route = NEW_DEVICE_NOTICE_TWO_FACTOR_ROUTE,
    ) {
        NewDeviceNoticeTwoFactorScreen(
            onNavigateBackToVault = onNavigateBackToVault,
        )
    }
}

package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS = "email_address"
private const val NEW_DEVICE_NOTICE_PREFIX = "new_device_notice"
private const val NEW_DEVICE_NOTICE_EMAIL_ACCESS_ROUTE =
    "$NEW_DEVICE_NOTICE_PREFIX/{${EMAIL_ADDRESS}}"

/**
 * Class to retrieve new device notice email access arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class NewDeviceNoticeEmailAccessArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EMAIL_ADDRESS]) as String,
    )
}

/**
 * Navigate to the new device notice email access screen.
 */
fun NavController.navigateToNewDeviceNoticeEmailAccess(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$NEW_DEVICE_NOTICE_PREFIX/$emailAddress",
        navOptions = navOptions,
    )
}

/**
 * Add the new device notice email access screen to the nav graph.
 */
fun NavGraphBuilder.newDeviceNoticeEmailAccessDestination(
    onNavigateBackToVault: () -> Unit,
    onNavigateToTwoFactorOptions: () -> Unit,
) {
    composableWithSlideTransitions(
        route = NEW_DEVICE_NOTICE_EMAIL_ACCESS_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        NewDeviceNoticeEmailAccessScreen(
            onNavigateBackToVault = onNavigateBackToVault,
            onNavigateToTwoFactorOptions = onNavigateToTwoFactorOptions,
        )
    }
}

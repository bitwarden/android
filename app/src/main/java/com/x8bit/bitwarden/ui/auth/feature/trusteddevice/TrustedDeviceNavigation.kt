package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val EMAIL_ADDRESS: String = "email_address"
private const val TRUSTED_DEVICE_PREFIX: String = "trusted_device"
private const val TRUSTED_DEVICE_ROUTE: String = "$TRUSTED_DEVICE_PREFIX/{${EMAIL_ADDRESS}}"

/**
 * Class to retrieve trusted device arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class TrustedDeviceArgs(val emailAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        emailAddress = checkNotNull(savedStateHandle.get<String>(EMAIL_ADDRESS)),
    )
}

/**
 * Add the Trusted Device Screen to the nav graph.
 */
fun NavGraphBuilder.trustedDeviceDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = TRUSTED_DEVICE_ROUTE,
        arguments = listOf(
            navArgument(EMAIL_ADDRESS) { type = NavType.StringType },
        ),
    ) {
        TrustedDeviceScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Trusted Device Screen.
 */
fun NavController.navigateToTrustedDevice(
    emailAddress: String,
    navOptions: NavOptions? = null,
) {
    this.navigate("$TRUSTED_DEVICE_PREFIX/$emailAddress", navOptions)
}

package com.x8bit.bitwarden.ui.auth.feature.trusteddevice.handlers

import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceAction
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceViewModel

/**
 * A collection of handler functions for managing actions within the context of the Trusted Device
 * Screen.
 */
data class TrustedDeviceHandlers(
    val onBackClick: () -> Unit,
) {
    companion object {
        /**
         * Creates an instance of [TrustedDeviceHandlers] by binding actions to the provided
         * [TrustedDeviceViewModel].
         */
        fun create(viewModel: TrustedDeviceViewModel): TrustedDeviceHandlers =
            TrustedDeviceHandlers(
                onBackClick = { viewModel.trySendAction(TrustedDeviceAction.BackClick) },
            )
    }
}

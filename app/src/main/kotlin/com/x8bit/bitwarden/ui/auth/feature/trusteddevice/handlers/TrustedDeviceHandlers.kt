package com.x8bit.bitwarden.ui.auth.feature.trusteddevice.handlers

import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceAction
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.TrustedDeviceViewModel

/**
 * A collection of handler functions for managing actions within the context of the Trusted Device
 * Screen.
 */
data class TrustedDeviceHandlers(
    val onBackClick: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onRememberToggle: (Boolean) -> Unit,
    val onContinueClick: () -> Unit,
    val onApproveWithDeviceClick: () -> Unit,
    val onApproveWithAdminClick: () -> Unit,
    val onApproveWithPasswordClick: () -> Unit,
    val onNotYouButtonClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [TrustedDeviceHandlers] by binding actions to the provided
         * [TrustedDeviceViewModel].
         */
        fun create(viewModel: TrustedDeviceViewModel): TrustedDeviceHandlers =
            TrustedDeviceHandlers(
                onBackClick = { viewModel.trySendAction(TrustedDeviceAction.BackClick) },
                onDismissDialog = { viewModel.trySendAction(TrustedDeviceAction.DismissDialog) },
                onRememberToggle = {
                    viewModel.trySendAction(TrustedDeviceAction.RememberToggle(it))
                },
                onContinueClick = { viewModel.trySendAction(TrustedDeviceAction.ContinueClick) },
                onApproveWithDeviceClick = {
                    viewModel.trySendAction(TrustedDeviceAction.ApproveWithDeviceClick)
                },
                onApproveWithAdminClick = {
                    viewModel.trySendAction(TrustedDeviceAction.ApproveWithAdminClick)
                },
                onApproveWithPasswordClick = {
                    viewModel.trySendAction(TrustedDeviceAction.ApproveWithPasswordClick)
                },
                onNotYouButtonClick = {
                    viewModel.trySendAction(TrustedDeviceAction.NotYouClick)
                },
            )
    }
}

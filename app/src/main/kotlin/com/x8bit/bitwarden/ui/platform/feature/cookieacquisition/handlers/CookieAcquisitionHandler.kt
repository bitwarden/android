package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.CookieAcquisitionAction
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.CookieAcquisitionViewModel

/**
 * A class to handle user interactions for the Cookie Acquisition screen.
 */
data class CookieAcquisitionHandler(
    val onLaunchBrowserClick: () -> Unit,
    val onContinueWithoutSyncingClick: () -> Unit,
    val onWhyAmISeeingThisClick: () -> Unit,
    val onDismissDialogClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [CookieAcquisitionHandler] using the provided
         * [CookieAcquisitionViewModel].
         */
        fun create(
            viewModel: CookieAcquisitionViewModel,
        ): CookieAcquisitionHandler =
            CookieAcquisitionHandler(
                onLaunchBrowserClick = {
                    viewModel.trySendAction(
                        CookieAcquisitionAction.LaunchBrowserClick,
                    )
                },
                onContinueWithoutSyncingClick = {
                    viewModel.trySendAction(
                        CookieAcquisitionAction.ContinueWithoutSyncingClick,
                    )
                },
                onWhyAmISeeingThisClick = {
                    viewModel.trySendAction(
                        CookieAcquisitionAction.WhyAmISeeingThisClick,
                    )
                },
                onDismissDialogClick = {
                    viewModel.trySendAction(
                        CookieAcquisitionAction.DismissDialogClick,
                    )
                },
            )
    }
}

/**
 * Helper function to create and remember a [CookieAcquisitionHandler] instance.
 */
@Composable
fun rememberCookieAcquisitionHandler(
    viewModel: CookieAcquisitionViewModel,
): CookieAcquisitionHandler = remember(viewModel) {
    CookieAcquisitionHandler.create(viewModel)
}

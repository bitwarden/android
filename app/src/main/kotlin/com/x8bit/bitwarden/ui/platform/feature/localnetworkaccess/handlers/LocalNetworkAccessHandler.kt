package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.LocalNetworkAccessAction
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.LocalNetworkAccessViewModel

/**
 * A class to handle user interactions for the Local Network Access screen.
 */
data class LocalNetworkAccessHandler(
    val onCloseClick: () -> Unit,
    val onContinueWithoutPermissionClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onResumed: (hasPermission: Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [LocalNetworkAccessHandler] using the provided
         * [LocalNetworkAccessViewModel].
         */
        fun create(
            viewModel: LocalNetworkAccessViewModel,
        ): LocalNetworkAccessHandler = LocalNetworkAccessHandler(
            onCloseClick = { viewModel.trySendAction(LocalNetworkAccessAction.CloseClick) },
            onContinueWithoutPermissionClick = {
                viewModel.trySendAction(LocalNetworkAccessAction.ContinueWithoutPermissionClick)
            },
            onSettingsClick = { viewModel.trySendAction(LocalNetworkAccessAction.SettingsClick) },
            onResumed = { viewModel.trySendAction(LocalNetworkAccessAction.Resumed(it)) },
        )
    }
}

/**
 * Helper function to create and remember a [LocalNetworkAccessHandler] instance.
 */
@Composable
fun rememberLocalNetworkAccessHandler(
    viewModel: LocalNetworkAccessViewModel,
): LocalNetworkAccessHandler = remember(viewModel) {
    LocalNetworkAccessHandler.create(viewModel)
}

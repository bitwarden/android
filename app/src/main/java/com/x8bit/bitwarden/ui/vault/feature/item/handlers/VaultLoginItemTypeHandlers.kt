package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing login
 * items in a vault.
 */
@Suppress("LongParameterList")
data class VaultLoginItemTypeHandlers(
    val onCheckForBreachClick: () -> Unit,
    val onCopyPasswordClick: () -> Unit,
    val onCopyTotpCodeClick: () -> Unit,
    val onCopyUriClick: (String) -> Unit,
    val onCopyUsernameClick: () -> Unit,
    val onLaunchUriClick: (String) -> Unit,
    val onPasswordHistoryClick: () -> Unit,
    val onShowPasswordClick: (isVisible: Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates the [VaultLoginItemTypeHandlers] using the [viewModel] to send desired actions.
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultItemViewModel,
        ): VaultLoginItemTypeHandlers =
            VaultLoginItemTypeHandlers(
                onCheckForBreachClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.CheckForBreachClick)
                },
                onCopyPasswordClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)
                },
                onCopyTotpCodeClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyTotpClick)
                },
                onCopyUriClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUriClick(it))
                },
                onCopyUsernameClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)
                },
                onLaunchUriClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.LaunchClick(it))
                },
                onPasswordHistoryClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
                },
                onShowPasswordClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Login.PasswordVisibilityClicked(it),
                    )
                },
            )
    }
}

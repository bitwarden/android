package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions common within the context of viewing
 * items in a vault.
 *
 * @property onCopyCustomHiddenField
 * @property onCopyCustomTextField
 * @property onShowHiddenFieldClick
 */
data class VaultCommonItemTypeHandlers(
    val onRefreshClick: () -> Unit,
    val onCopyCustomHiddenField: (String) -> Unit,
    val onCopyCustomTextField: (String) -> Unit,
    val onShowHiddenFieldClick: (
        VaultItemState.ViewState.Content.Common.Custom.HiddenField,
        Boolean,
    ) -> Unit,
    val onAttachmentDownloadClick: (VaultItemState.ViewState.Content.Common.AttachmentItem) -> Unit,
    val onCopyNotesClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultCommonItemTypeHandlers] by binding actions
         * to the provided [VaultItemViewModel].
         */
        fun create(
            viewModel: VaultItemViewModel,
        ): VaultCommonItemTypeHandlers =
            VaultCommonItemTypeHandlers(
                onRefreshClick = {
                    viewModel.trySendAction(VaultItemAction.Common.RefreshClick)
                },
                onCopyCustomHiddenField = {
                    viewModel.trySendAction(VaultItemAction.Common.CopyCustomHiddenFieldClick(it))
                },
                onCopyCustomTextField = {
                    viewModel.trySendAction(VaultItemAction.Common.CopyCustomTextFieldClick(it))
                },
                onShowHiddenFieldClick = { customField, isVisible ->
                    viewModel.trySendAction(
                        VaultItemAction.Common.HiddenFieldVisibilityClicked(
                            isVisible = isVisible,
                            field = customField,
                        ),
                    )
                },
                onAttachmentDownloadClick = {
                    viewModel.trySendAction(VaultItemAction.Common.AttachmentDownloadClick(it))
                },
                onCopyNotesClick = {
                    viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
                },
            )
    }
}

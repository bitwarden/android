package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Default, "select" Text to show on multi select buttons in the VaultAddEdit package.
 */
val SELECT_TEXT: Text
    get() = "-- "
        .asText()
        .concat(R.string.select.asText())
        .concat(" --".asText())

/**
 * Transforms a [VaultItemCipherType] into [VaultAddEditState.ViewState.Content.ItemType].
 */
fun VaultItemCipherType.toItemType(): VaultAddEditState.ViewState.Content.ItemType =
    when (this) {
        VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
        VaultItemCipherType.CARD -> VaultAddEditState.ViewState.Content.ItemType.Card()
        VaultItemCipherType.IDENTITY -> VaultAddEditState.ViewState.Content.ItemType.Identity()
        VaultItemCipherType.SECURE_NOTE -> VaultAddEditState.ViewState.Content.ItemType.SecureNotes
        VaultItemCipherType.SSH_KEY -> VaultAddEditState.ViewState.Content.ItemType.SshKey()
    }

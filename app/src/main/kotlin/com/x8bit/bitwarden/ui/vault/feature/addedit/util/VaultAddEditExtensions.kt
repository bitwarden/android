package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

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
        VaultItemCipherType.BANK_ACCOUNT -> {
            VaultAddEditState.ViewState.Content.ItemType.BankAccount()
        }
        VaultItemCipherType.DRIVERS_LICENSE -> {
            VaultAddEditState.ViewState.Content.ItemType.License()
        }
        VaultItemCipherType.PASSPORT -> {
            VaultAddEditState.ViewState.Content.ItemType.Passport()
        }
    }

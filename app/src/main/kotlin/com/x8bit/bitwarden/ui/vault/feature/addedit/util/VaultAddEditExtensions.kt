package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState.ViewState.Content.ItemType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Default, "select" Text to show on multi select buttons in the VaultAddEdit package.
 */
val SELECT_TEXT: Text
    get() = "-- "
        .asText()
        .concat(BitwardenString.select.asText())
        .concat(" --".asText())

/**
 * Transforms a [VaultItemCipherType] into [VaultAddEditState.ViewState.Content.ItemType].
 */
fun VaultItemCipherType.toItemType(): ItemType =
    when (this) {
        VaultItemCipherType.LOGIN -> ItemType.Login()
        VaultItemCipherType.CARD -> ItemType.Card()
        VaultItemCipherType.IDENTITY -> ItemType.Identity()
        VaultItemCipherType.SECURE_NOTE -> ItemType.SecureNotes
        VaultItemCipherType.SSH_KEY -> ItemType.SshKey()
        VaultItemCipherType.BANK_ACCOUNT -> ItemType.BankAccount()
        VaultItemCipherType.DRIVERS_LICENSE -> ItemType.DriversLicense()
        VaultItemCipherType.PASSPORT -> ItemType.Passport()
    }

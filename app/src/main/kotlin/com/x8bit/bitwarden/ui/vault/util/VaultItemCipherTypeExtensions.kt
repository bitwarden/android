package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * The message to display in a snackbar after a cipher of this type is saved.
 */
val VaultItemCipherType.savedSnackbarMessage: Text
    get() = when (this) {
        VaultItemCipherType.LOGIN -> BitwardenString.login_saved.asText()
        VaultItemCipherType.CARD -> BitwardenString.card_saved.asText()
        VaultItemCipherType.IDENTITY -> BitwardenString.identity_saved.asText()
        VaultItemCipherType.SECURE_NOTE -> BitwardenString.secure_note_saved.asText()
        VaultItemCipherType.SSH_KEY -> BitwardenString.ssh_key_saved.asText()
        VaultItemCipherType.BANK_ACCOUNT -> BitwardenString.bank_account_saved.asText()
        VaultItemCipherType.DRIVERS_LICENSE -> BitwardenString.license_saved.asText()
        VaultItemCipherType.PASSPORT -> BitwardenString.passport_saved.asText()
    }

package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultItemCipherTypeExtensionsTest {

    @Test
    fun `savedSnackbarMessage should return the correct value for each VaultItemCipherType`() {
        mapOf(
            VaultItemCipherType.LOGIN to BitwardenString.login_saved.asText(),
            VaultItemCipherType.CARD to BitwardenString.card_saved.asText(),
            VaultItemCipherType.IDENTITY to BitwardenString.identity_saved.asText(),
            VaultItemCipherType.SECURE_NOTE to BitwardenString.secure_note_saved.asText(),
            VaultItemCipherType.SSH_KEY to BitwardenString.ssh_key_saved.asText(),
            VaultItemCipherType.BANK_ACCOUNT to BitwardenString.bank_account_saved.asText(),
            VaultItemCipherType.DRIVERS_LICENSE to BitwardenString.license_saved.asText(),
            VaultItemCipherType.PASSPORT to BitwardenString.passport_saved.asText(),
        )
            .forEach { (type, message) ->
                assertEquals(
                    message,
                    type.savedSnackbarMessage,
                )
            }
    }
}

package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Converts the [CipherType] to its corresponding [VaultItemCipherType].
 */
fun CipherType.toVaultItemCipherType(): VaultItemCipherType =
    when (this) {
        CipherType.LOGIN -> VaultItemCipherType.LOGIN
        CipherType.SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
        CipherType.CARD -> VaultItemCipherType.CARD
        CipherType.IDENTITY -> VaultItemCipherType.IDENTITY
        CipherType.SSH_KEY -> VaultItemCipherType.SSH_KEY
    }

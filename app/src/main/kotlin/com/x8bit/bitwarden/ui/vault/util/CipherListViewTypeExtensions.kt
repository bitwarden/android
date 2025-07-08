package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Converts the [CipherType] to its corresponding [VaultItemCipherType].
 */
fun CipherListViewType.toVaultItemCipherType(): VaultItemCipherType =
    when (this) {
        is CipherListViewType.Login -> VaultItemCipherType.LOGIN
        CipherListViewType.SecureNote -> VaultItemCipherType.SECURE_NOTE
        is CipherListViewType.Card -> VaultItemCipherType.CARD
        CipherListViewType.Identity -> VaultItemCipherType.IDENTITY
        CipherListViewType.SshKey -> VaultItemCipherType.SSH_KEY
    }

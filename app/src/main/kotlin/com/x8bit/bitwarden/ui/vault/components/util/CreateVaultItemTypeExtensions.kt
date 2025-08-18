package com.x8bit.bitwarden.ui.vault.components.util

import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Extension function to map [CreateVaultItemType] to a matching [VaultItemCipherType] or
 * `null` if there is no matching type.
 */
fun CreateVaultItemType.toVaultItemCipherTypeOrNull(): VaultItemCipherType? = when (this) {
    CreateVaultItemType.LOGIN -> VaultItemCipherType.LOGIN
    CreateVaultItemType.CARD -> VaultItemCipherType.CARD
    CreateVaultItemType.IDENTITY -> VaultItemCipherType.IDENTITY
    CreateVaultItemType.SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
    CreateVaultItemType.SSH_KEY -> VaultItemCipherType.SSH_KEY
    CreateVaultItemType.FOLDER -> null
}

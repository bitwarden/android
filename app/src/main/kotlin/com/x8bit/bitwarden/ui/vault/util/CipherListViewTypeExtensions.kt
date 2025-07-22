package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType

/**
 * Converts the [CipherListViewType] to its corresponding [CipherType].
 */
fun CipherListViewType.toSdkCipherType(): CipherType =
    when (this) {
        is CipherListViewType.Card -> CipherType.CARD
        CipherListViewType.Identity -> CipherType.IDENTITY
        is CipherListViewType.Login -> CipherType.LOGIN
        CipherListViewType.SecureNote -> CipherType.SECURE_NOTE
        CipherListViewType.SshKey -> CipherType.SSH_KEY
    }

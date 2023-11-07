package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult

/**
 * Transform a [InitializeCryptoResult] to [VaultUnlockResult].
 */
fun InitializeCryptoResult.toVaultUnlockResult(): VaultUnlockResult =
    when (this) {
        InitializeCryptoResult.AuthenticationError -> VaultUnlockResult.AuthenticationError
        InitializeCryptoResult.Success -> VaultUnlockResult.Success
    }

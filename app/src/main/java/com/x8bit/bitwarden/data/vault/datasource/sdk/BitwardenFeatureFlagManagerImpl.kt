package com.x8bit.bitwarden.data.vault.datasource.sdk

private const val CIPHER_KEY_ENCRYPTION_KEY = "enableCipherKeyEncryption"

/**
 * Primary implementation of [BitwardenFeatureFlagManager].
 */
class BitwardenFeatureFlagManagerImpl : BitwardenFeatureFlagManager {
    override val featureFlags: Map<String, Boolean>
        get() = mapOf(CIPHER_KEY_ENCRYPTION_KEY to true)
}

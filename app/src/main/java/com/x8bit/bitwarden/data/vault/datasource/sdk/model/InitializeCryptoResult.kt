package com.x8bit.bitwarden.data.vault.datasource.sdk.model

/**
 * Models result of initializing cryptography functionality for the Bitwarden SDK.
 */
sealed class InitializeCryptoResult {

    /**
     * Successfully initialized cryptography functionality.
     */
    data object Success : InitializeCryptoResult()

    /**
     * Incorrect password or key(s) provided.
     */
    data class AuthenticationError(
        val message: String? = null,
    ) : InitializeCryptoResult()
}

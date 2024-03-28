package com.x8bit.bitwarden.authenticator.data.authenticator.repository.model

import com.bitwarden.core.CipherView

/**
 * Represents decrypted authenticator data.
 *
 * @property ciphers List of decrypted ciphers.
 */
data class AuthenticatorData(
    val ciphers: List<CipherView>,
)

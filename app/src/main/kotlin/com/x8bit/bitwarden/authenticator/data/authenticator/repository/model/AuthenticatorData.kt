package com.x8bit.bitwarden.authenticator.data.authenticator.repository.model

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity

/**
 * Represents decrypted authenticator data.
 *
 * @property items List of decrypted authenticator items.
 */
data class AuthenticatorData(
    val items: List<AuthenticatorItemEntity>,
)

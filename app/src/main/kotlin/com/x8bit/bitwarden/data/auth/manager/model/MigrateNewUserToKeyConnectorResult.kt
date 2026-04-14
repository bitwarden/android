package com.x8bit.bitwarden.data.auth.manager.model

import com.bitwarden.core.WrappedAccountCryptographicState

/**
 * Models result of migrating a new user to key connector.
 * */
data class MigrateNewUserToKeyConnectorResult(
    val masterKey: String,
    val encryptedUserKey: String,
    val privateKey: String,
    val accountCryptographicState: WrappedAccountCryptographicState,
)

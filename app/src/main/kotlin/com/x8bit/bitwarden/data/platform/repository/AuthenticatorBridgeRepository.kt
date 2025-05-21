package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.authenticatorbridge.model.SharedAccountData

/**
 * Provides an API for querying disk sources required by Authenticator Bridge
 * service implementation.
 */
interface AuthenticatorBridgeRepository {

    /**
     * The currently persisted authenticator sync symmetric key. This key is used for
     * encrypting IPC traffic. This will return null if no users have enabled authenticator sync.
     */
    val authenticatorSyncSymmetricKey: ByteArray?

    /**
     * Get a list of shared account data. This function will go through all accounts and for each
     * one, check to see if the user has Authenticator account syncing enabled and if they
     * do, it will query and decrypt the user's shared account data.
     *
     * Users who do not have authenticator sync enabled or otherwise cannot have their ciphers
     * accessed will be omitted from the list.
     */
    suspend fun getSharedAccounts(): SharedAccountData
}

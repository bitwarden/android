package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.bridge.model.SharedAccountData

/**
 * Provides an API for querying disk sources required by [BridgeRepositoryImpl].
 *
 * Note: this repository should not be injected anywhere other than [BridgeRepositoryImpl].
 */
interface BridgeRepository {

    /**
     * The currently persisted authenticator sync symmetric key. This key is used for
     * encrypting IPC traffic.
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

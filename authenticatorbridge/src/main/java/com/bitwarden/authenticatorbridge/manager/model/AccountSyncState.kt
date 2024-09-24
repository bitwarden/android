package com.bitwarden.authenticatorbridge.manager.model

import com.bitwarden.authenticatorbridge.model.SharedAccountData

/**
 * Models various states of account syncing.
 */
sealed class AccountSyncState {

    /**
     * Something went wrong syncing accounts.
     */
    data object Error : AccountSyncState()

    /**
     * The user needs to enable authenticator syncing from the bitwarden app.
     */
    data object SyncNotEnabled : AccountSyncState()

    /**
     * Accounts are being synced.
     */
    data object Loading : AccountSyncState()

    /**
     * Accounts successfully synced.
     */
    data class Success(val accounts: List<SharedAccountData.Account>) : AccountSyncState()
}

package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of deleting an account.
 */
sealed class DeleteAccountResult {
    /**
     * Delete succeeded.
     */
    data object Success : DeleteAccountResult()

    /**
     * There was an error deleting the account.
     */
    data object Error : DeleteAccountResult()
}

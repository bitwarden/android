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
     * There was an error deleting the account owned by organization.
     */
    data object CannotDeleteAccountOwnedByOrg : DeleteAccountResult()

    /**
     * There was an error deleting the account.
     */
    data class Error(
        val message: String?,
        val error: Throwable?,
    ) : DeleteAccountResult()
}

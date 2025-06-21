package com.x8bit.bitwarden.data.credentials.model

import com.bitwarden.vault.CipherView

/**
 * Models the data returned from creating a Password credential.
 */

sealed class PasswordRegisterCredentialResult {

    /**
     * Indicates the credential has been successfully registered.
     */
    data class Success(
        val data: CipherView,
    ) : PasswordRegisterCredentialResult()

    /**
     * Indicates there was an error and the credential was not registered.
     */
    sealed class Error : PasswordRegisterCredentialResult() {

        /**
         * Indicates an internal error occurred.
         */
        data object InternalError : Error()
    }
}

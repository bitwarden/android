package com.x8bit.bitwarden.data.credentials.model

/**
 * Models the data returned from creating a Password credential.
 */

sealed class PasswordRegisterResult {

    /**
     * Indicates the credential has been successfully registered.
     */
    data object Success : PasswordRegisterResult()

    /**
     * Indicates there was an error and the credential was not registered.
     */
    sealed class Error : PasswordRegisterResult() {

        /**
         * Indicates an internal error occurred.
         */
        data object InternalError : Error()
    }
}

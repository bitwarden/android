package com.x8bit.bitwarden.data.autofill.password.model

/**
 * Models the data returned from creating a Password credential.
 */
sealed class PasswordRegisterCredentialResult {

    /**
     * Indicates the credential has been successfully registered.
     */
    data object Success : PasswordRegisterCredentialResult()

    /**
     * Indicates there was an error and the credential was not registered.
     */
    data object Error : PasswordRegisterCredentialResult()

}

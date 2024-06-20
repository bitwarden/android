package com.x8bit.bitwarden.data.vault.datasource.sdk.model

/**
 * Models the result of saving a FIDO 2 credential.
 */
sealed class SaveCredentialResult {

    /**
     * Indicates the credential has been saved.
     */
    data object Success : SaveCredentialResult()

    /**
     * Indicates the credential was not saved.
     */
    data object Error : SaveCredentialResult()
}

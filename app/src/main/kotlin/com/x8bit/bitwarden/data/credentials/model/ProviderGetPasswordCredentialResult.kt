package com.x8bit.bitwarden.data.credentials.model

import com.bitwarden.vault.LoginView

/**
 * Represents possible outcomes of a Password credential assertion request.
 */
sealed class ProviderGetPasswordCredentialResult {

    /**
     * Indicates the assertion request completed and [LoginView] was successfully retrieved.
     */
    data class Success(val credential: LoginView) : ProviderGetPasswordCredentialResult()

    /**
     * Indicates there was an error and the assertion was not successful.
     */
    data object Error : ProviderGetPasswordCredentialResult()

}

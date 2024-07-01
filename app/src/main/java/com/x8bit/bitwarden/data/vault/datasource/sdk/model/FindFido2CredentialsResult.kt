package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.CipherView

/**
 * Models the result of querying for ciphers with FIDO 2 credentials.
 */
sealed class FindFido2CredentialsResult {

    /**
     * Indicates the query was executed successfully.
     */
    data class Success(val cipherViews: List<CipherView>) : FindFido2CredentialsResult()

    /**
     * Indicates the query was not executed successfully.
     */
    data object Error : FindFido2CredentialsResult()
}

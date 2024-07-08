package com.x8bit.bitwarden.data.autofill.fido2.manager

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.PublicKeyCredentialCreationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult

/**
 * Responsible for managing FIDO 2 credential creation and authentication.
 */
interface Fido2CredentialManager {

    /**
     * Attempt to validate the RP and origin of the provided [fido2CredentialRequest].
     */
    suspend fun validateOrigin(
        fido2CredentialRequest: Fido2CredentialRequest,
    ): Fido2ValidateOriginResult

    /**
     * Attempt to extract FIDO 2 passkey creation options from the system [requestJson], or null.
     */
    fun getPasskeyCreateOptionsOrNull(
        requestJson: String,
    ): PublicKeyCredentialCreationOptions?

    /**
     * Register a new FIDO 2 credential to a users vault.
     */
    suspend fun registerFido2Credential(
        userId: String,
        fido2CredentialRequest: Fido2CredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult
}

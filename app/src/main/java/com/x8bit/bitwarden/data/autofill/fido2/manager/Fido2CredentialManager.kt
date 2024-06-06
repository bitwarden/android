package com.x8bit.bitwarden.data.autofill.fido2.manager

import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialResult
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
     * Attempt to create a FIDO2 credential from the given [credentialRequest] and associate it to
     * the given [cipherView].
     */
    fun createCredentialForCipher(
        credentialRequest: Fido2CredentialRequest,
        cipherView: CipherView,
    ): Fido2CreateCredentialResult
}

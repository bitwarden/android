package com.x8bit.bitwarden.data.autofill.fido2.manager

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions

/**
 * Responsible for managing FIDO 2 credential registration and authentication.
 */
interface Fido2CredentialManager {
    /**
     * Returns true when the user has performed an explicit verification action. E.g., biometric
     * verification, device credential verification, or vault unlock.
     */
    var isUserVerified: Boolean

    /**
     * The number of times the user has attempted to authenticate with their password or PIN
     * for the FIDO 2 user verification flow.
     */
    var authenticationAttempts: Int

    /**
     * Attempt to validate the RP and origin of the provided [fido2CredentialRequest].
     */
    suspend fun validateOrigin(
        fido2CredentialRequest: Fido2CredentialRequest,
    ): Fido2ValidateOriginResult

    /**
     * Attempt to extract FIDO 2 passkey attestation options from the system [requestJson], or null.
     */
    fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions?

    /**
     * Register a new FIDO 2 credential to a users vault.
     */
    suspend fun registerFido2Credential(
        userId: String,
        fido2CredentialRequest: Fido2CredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult
}

package com.x8bit.bitwarden.data.autofill.fido2.manager

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
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
     * Attempt to extract FIDO 2 passkey attestation options from the system [requestJson], or null.
     */
    fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions?

    /**
     * Attempt to extract FIDO 2 passkey assertion options from the system [requestJson], or null.
     */
    fun getPasskeyAssertionOptionsOrNull(
        requestJson: String,
    ): PasskeyAssertionOptions?

    /**
     * Register a new FIDO 2 credential to a users vault.
     */
    suspend fun registerFido2Credential(
        userId: String,
        fido2CreateCredentialRequest: Fido2CreateCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult

    /**
     * Authenticate a FIDO credential against a cipher in the users vault.
     */
    suspend fun authenticateFido2Credential(
        userId: String,
        request: Fido2CredentialAssertionRequest,
        selectedCipherView: CipherView,
    ): Fido2CredentialAssertionResult

    /**
     * Whether or not the user has authentication attempts remaining.
     */
    fun hasAuthenticationAttemptsRemaining(): Boolean
}

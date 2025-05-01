package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement

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
        callingAppInfo: CallingAppInfo,
        createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult

    /**
     * Authenticate a FIDO credential against a cipher in the users vault.
     */
    suspend fun authenticateFido2Credential(
        userId: String,
        callingAppInfo: CallingAppInfo,
        request: GetPublicKeyCredentialOption,
        selectedCipherView: CipherView,
        origin: String?,
    ): Fido2CredentialAssertionResult

    /**
     * Whether or not the user has authentication attempts remaining.
     */
    fun hasAuthenticationAttemptsRemaining(): Boolean

    /**
     * Determines the user verification requirement for a given FIDO2 assertion request.
     *
     * @param request The FIDO2 credential assertion request.
     * @param fallbackRequirement The fallback requirement to use if the request doesn't specify
     * one.
     * Defaults to [UserVerificationRequirement.REQUIRED].
     * @return The user verification requirement for the request.
     */
    fun getUserVerificationRequirement(
        request: ProviderGetCredentialRequest,
        fallbackRequirement: UserVerificationRequirement = UserVerificationRequirement.REQUIRED,
    ): UserVerificationRequirement

    /**
     * Determines the user verification requirement for a given FIDO2 registration request.
     *
     * @param request The FIDO2 credential request.
     * @param fallbackRequirement The fallback requirement to use if the request doesn't specify
     * one.
     * Defaults to [UserVerificationRequirement.REQUIRED].
     * @return The user verification requirement for the request.
     */
    fun getUserVerificationRequirement(
        request: CreatePublicKeyCredentialRequest,
        fallbackRequirement: UserVerificationRequirement = UserVerificationRequirement.REQUIRED,
    ): UserVerificationRequirement

    /**
     * Retrieve a list of [CredentialEntry] objects representing vault items matching the given
     * request [option].
     */
    suspend fun getPublicKeyCredentialEntries(
        userId: String,
        option: BeginGetPublicKeyCredentialOption,
    ): Result<List<CredentialEntry>>
}

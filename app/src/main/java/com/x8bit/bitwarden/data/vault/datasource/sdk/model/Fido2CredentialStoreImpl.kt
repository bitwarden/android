package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import androidx.credentials.exceptions.CreateCredentialUnknownException
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Primary implementation of [Fido2CredentialStore].
 */
@OmitFromCoverage
class Fido2CredentialStoreImpl(
    private val cipherViews: List<CipherView>,
    private val findFido2Credentials: suspend (
        credentialIds: List<ByteArray>,
        relyingPartyId: String,
    ) -> FindFido2CredentialsResult,
    private val saveCipher: suspend (cipher: Cipher) -> SaveCredentialResult,
) : Fido2CredentialStore {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun findCredentials(
        fido2CredentialIds: List<ByteArray>?,
        relyingPartyId: String,
    ): List<CipherView> =
        when (val result = findFido2Credentials(fido2CredentialIds.orEmpty(), relyingPartyId)) {
            is FindFido2CredentialsResult.Error -> {
                // This exception is caught and handled by the SDK, which
                // uses it to produce a FIDO 2 spec compliant attestation
                // error.
                throw CreateCredentialUnknownException()
            }

            is FindFido2CredentialsResult.Success -> {
                result.cipherViews
            }
        }

    override suspend fun saveCredential(cred: Cipher) {
        if (saveCipher(cred) is SaveCredentialResult.Error) {
            // This exception is caught and handled by the SDK, which uses
            // it to produce a FIDO 2 spec compliant attestation error.
            throw CreateCredentialUnknownException()
        }
    }

    override suspend fun allCredentials(): List<CipherView> = cipherViews
}

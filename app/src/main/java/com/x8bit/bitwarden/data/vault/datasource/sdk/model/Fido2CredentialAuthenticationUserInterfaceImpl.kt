package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.CheckUserOptions
import com.bitwarden.sdk.CheckUserResult
import com.bitwarden.sdk.CipherViewWrapper
import com.bitwarden.sdk.Fido2UserInterface
import com.bitwarden.sdk.UiHint
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2CredentialNewView
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Implementation of [Fido2UserInterface] for authenticating with a FIDO 2 credential.
 */
@OmitFromCoverage
class Fido2CredentialAuthenticationUserInterfaceImpl(
    private val isVerificationSupported: Boolean,
    private val selectedCipher: CipherView,
) : Fido2UserInterface {
    override suspend fun checkUser(
        options: CheckUserOptions,
        hint: UiHint,
    ): CheckUserResult = CheckUserResult(true, true)

    override suspend fun checkUserAndPickCredentialForCreation(
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ): CipherViewWrapper = throw IllegalStateException()

    override suspend fun isVerificationEnabled(): Boolean = isVerificationSupported

    override suspend fun pickCredentialForAuthentication(
        availableCredentials: List<CipherView>,
    ): CipherViewWrapper = CipherViewWrapper(selectedCipher)
}

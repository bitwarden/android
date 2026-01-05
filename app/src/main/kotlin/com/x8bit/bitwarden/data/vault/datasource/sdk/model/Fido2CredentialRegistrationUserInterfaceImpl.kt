package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.fido.CheckUserOptions
import com.bitwarden.sdk.CheckUserAndPickCredentialForCreationResult
import com.bitwarden.sdk.CheckUserResult
import com.bitwarden.sdk.CipherViewWrapper
import com.bitwarden.sdk.Fido2UserInterface
import com.bitwarden.sdk.UiHint
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2CredentialNewView

/**
 * Implementation of [Fido2UserInterface] for registering new FIDO 2 credentials.
 */
@OmitFromCoverage
class Fido2CredentialRegistrationUserInterfaceImpl(
    private val selectedCipherView: CipherView,
    private val isVerificationSupported: Boolean,
) : Fido2UserInterface {

    override suspend fun checkUser(
        options: CheckUserOptions,
        hint: UiHint,
    ): CheckUserResult = CheckUserResult(userPresent = true, userVerified = true)

    override suspend fun checkUserAndPickCredentialForCreation(
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ): CheckUserAndPickCredentialForCreationResult = CheckUserAndPickCredentialForCreationResult(
        cipher = CipherViewWrapper(selectedCipherView),
        checkUserResult = CheckUserResult(userPresent = true, userVerified = true),
    )

    override fun isVerificationEnabled(): Boolean = isVerificationSupported

    override suspend fun pickCredentialForAuthentication(
        availableCredentials: List<CipherView>,
    ): CipherViewWrapper = throw IllegalStateException()
}

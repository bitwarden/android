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
 * Implementation of [Fido2UserInterface] for registering new FIDO 2 credentials.
 */
@OmitFromCoverage
class Fido2CredentialRegistrationUserInterfaceImpl(
    private val isVerificationSupported: Boolean,
    private val checkUser: suspend (CheckUserOptions, UiHint?) -> CheckUserResult,
    private val checkUserAndPickCredentialForCreation: suspend (
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ) -> CipherViewWrapper,
) : Fido2UserInterface {

    override suspend fun checkUser(
        options: CheckUserOptions,
        hint: UiHint,
    ): CheckUserResult = checkUser.invoke(options, hint)

    override suspend fun checkUserAndPickCredentialForCreation(
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ): CipherViewWrapper = checkUserAndPickCredentialForCreation.invoke(options, newCredential)

    override suspend fun isVerificationEnabled(): Boolean = isVerificationSupported

    override suspend fun pickCredentialForAuthentication(
        availableCredentials: List<CipherView>,
    ): CipherViewWrapper = throw IllegalStateException()
}

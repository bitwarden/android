package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.CheckUserOptions
import com.bitwarden.sdk.CheckUserAndPickCredentialForCreationResult
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
    private val selectedCipherView: CipherView,
) : Fido2UserInterface {

    /**
     * Implicitly returns a [CheckUserResult] indicating the user is present and verified.
     *
     * The [options] parameter is ignored. User verification is performed prior to invoking the SDK.
     */
    override suspend fun checkUser(
        options: CheckUserOptions,
        hint: UiHint,
    ): CheckUserResult = CheckUserResult(true, true)

    /**
     * Returns a [CipherViewWrapper] containing the [selectedCipherView] the [newCredential] will be
     * registered to.
     */
    override suspend fun checkUserAndPickCredentialForCreation(
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ): CheckUserAndPickCredentialForCreationResult = CheckUserAndPickCredentialForCreationResult(
        cipher = CipherViewWrapper(selectedCipherView),
        checkUserResult = CheckUserResult(
            userPresent = true,
            userVerified = true,
        ),
    )

    override suspend fun isVerificationEnabled(): Boolean = isVerificationSupported

    /**
     * Throws an [IllegalStateException] as it should not be invoked during FIDO 2 credential
     * registration. Throwing an exception allows the SDK to gracefully terminate the ongoing
     * process and return a spec compliant error.
     */
    override suspend fun pickCredentialForAuthentication(
        availableCredentials: List<CipherView>,
    ): CipherViewWrapper = throw IllegalStateException()
}

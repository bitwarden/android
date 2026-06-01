package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.CheckUserOptions
import com.bitwarden.sdk.CheckUserAndPickCredentialForCreationResult
import com.bitwarden.sdk.CheckUserResult
import com.bitwarden.sdk.CipherViewWrapper
import com.bitwarden.sdk.Fido2UserInterface
import com.bitwarden.sdk.UiHint
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2CredentialNewView

/**
 * Implementation of [Fido2UserInterface] for searching for matching FIDO 2 credentials.
 */
class Fido2CredentialSearchUserInterfaceImpl : Fido2UserInterface {
    override suspend fun checkUser(
        options: CheckUserOptions,
        hint: UiHint,
    ): CheckUserResult =
        CheckUserResult(
            userPresent = true,
            userVerified = true,
        )

    override suspend fun checkUserAndPickCredentialForCreation(
        options: CheckUserOptions,
        newCredential: Fido2CredentialNewView,
    ): CheckUserAndPickCredentialForCreationResult = throw IllegalStateException()

    // Always return true for this property because any problems with verification should
    // be handled downstream where the app can actually offer verification methods.
    override fun isVerificationEnabled(): Boolean = true

    override suspend fun pickCredentialForAuthentication(
        availableCredentials: List<CipherView>,
    ): CipherViewWrapper = throw IllegalStateException()
}

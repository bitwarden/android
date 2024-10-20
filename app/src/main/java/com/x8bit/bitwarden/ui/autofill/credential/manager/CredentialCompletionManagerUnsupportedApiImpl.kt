package com.x8bit.bitwarden.ui.autofill.credential.manager

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * A no-op implementation of [Fido2CompletionManagerImpl] provided when the build version is below
 * UPSIDE_DOWN_CAKE (34). These versions do not support [androidx.credentials.CredentialProvider].
 */
object CredentialCompletionManagerUnsupportedApiImpl : CredentialCompletionManager {
    override fun completeFido2Registration(result: Fido2RegisterCredentialResult) = Unit

    override fun completeFido2Assertion(result: Fido2CredentialAssertionResult) = Unit
    override fun completePasswordRegistration(result: PasswordRegisterCredentialResult) = Unit

    override fun completePasswordAssertion(result: PasswordCredentialAssertionResult) = Unit

    override fun completeGetCredentialRequest(
        fido2Result: Fido2GetCredentialsResult?,
        passwordResult: PasswordGetCredentialsResult?,
    ) = Unit
}

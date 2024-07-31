package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult

/**
 * A no-op implementation of [Fido2CompletionManagerImpl] provided when the build version is below
 * UPSIDE_DOWN_CAKE (34). These versions do not support [androidx.credentials.CredentialProvider].
 */
object Fido2CompletionManagerUnsupportedApiImpl : Fido2CompletionManager {
    override fun completeFido2Registration(result: Fido2RegisterCredentialResult) = Unit

    override fun completeFido2Assertion(result: Fido2CredentialAssertionResult) = Unit

    override fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsResult) = Unit
}

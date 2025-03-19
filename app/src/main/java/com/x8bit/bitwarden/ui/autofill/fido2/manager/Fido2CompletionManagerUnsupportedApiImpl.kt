package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2AssertionCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2GetCredentialsCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2RegistrationCompletion

/**
 * A no-op implementation of [Fido2CompletionManagerImpl] provided when the build version is below
 * UPSIDE_DOWN_CAKE (34). These versions do not support [androidx.credentials.CredentialProvider].
 */
object Fido2CompletionManagerUnsupportedApiImpl : Fido2CompletionManager {
    override fun completeFido2Registration(result: Fido2RegistrationCompletion) = Unit

    override fun completeFido2Assertion(result: Fido2AssertionCompletion) = Unit

    override fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsCompletion) = Unit
}

package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.GetFido2CredentialsResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.RegisterFido2CredentialResult

/**
 * A no-op implementation of [Fido2CompletionManagerImpl] provided when the build version is below
 * UPSIDE_DOWN_CAKE (34). These versions do not support [androidx.credentials.CredentialProvider].
 */
object Fido2CompletionManagerUnsupportedApiImpl : Fido2CompletionManager {
    override fun completeFido2Registration(result: RegisterFido2CredentialResult) = Unit

    override fun completeFido2Assertion(result: AssertFido2CredentialResult) = Unit

    override fun completeFido2GetCredentialsRequest(result: GetFido2CredentialsResult) = Unit
}

package com.x8bit.bitwarden.ui.credentials.manager

import androidx.credentials.CredentialProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertPasswordCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.RegisterFido2CredentialResult

/**
 * A no-op implementation of [CredentialProviderCompletionManagerImpl] provided when the build
 * version is below UPSIDE_DOWN_CAKE (34). These versions do not support [CredentialProvider].
 */
@OmitFromCoverage
object CredentialProviderCompletionManagerUnsupportedApiImpl : CredentialProviderCompletionManager {
    override fun completeFido2Registration(result: RegisterFido2CredentialResult) = Unit

    override fun completeFido2Assertion(result: AssertFido2CredentialResult) = Unit

    override fun completePasswordAssertion(result: AssertPasswordCredentialResult) = Unit

    override fun completeProviderGetCredentialsRequest(result: GetCredentialsResult) = Unit
}

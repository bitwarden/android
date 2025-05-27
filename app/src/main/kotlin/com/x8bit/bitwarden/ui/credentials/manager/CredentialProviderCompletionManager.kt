package com.x8bit.bitwarden.ui.credentials.manager

import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertPasswordCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.RegisterFido2CredentialResult

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface CredentialProviderCompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: RegisterFido2CredentialResult)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: AssertFido2CredentialResult)

    /**
     * Complete the Password credential assertion process with the provided [result].
     */
    fun completePasswordAssertion(result: AssertPasswordCredentialResult)

    /**
     * Complete the CredentialManager "Get credentials" process with the provided [result].
     */
    fun completeProviderGetCredentialsRequest(result: GetCredentialsResult)
}

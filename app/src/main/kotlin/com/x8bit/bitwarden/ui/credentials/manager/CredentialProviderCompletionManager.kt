package com.x8bit.bitwarden.ui.credentials.manager

import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.RegisterFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.RegisterPasswordCredentialResult
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditEvent.CompletePasswordRegistration

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface CredentialProviderCompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: RegisterFido2CredentialResult)

    /**
     * Completes the Password registration process with the provided [result].
     */
    fun completePasswordRegistration(result: RegisterPasswordCredentialResult)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: AssertFido2CredentialResult)

    /**
     * Complete the Password credential retrieval process with the provided [result].
     */
    fun completePasswordGet(result: GetPasswordCredentialResult)

    /**
     * Complete the CredentialManager "Get credentials" process with the provided [result].
     */
    fun completeProviderGetCredentialsRequest(result: GetCredentialsResult)
}

package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.GetFido2CredentialsResult
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.RegisterFido2CredentialResult

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface Fido2CompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: RegisterFido2CredentialResult)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: AssertFido2CredentialResult)

    /**
     * Complete the FIDO 2 "Get credentials" process with the provided [result].
     */
    fun completeFido2GetCredentialsRequest(result: GetFido2CredentialsResult)
}

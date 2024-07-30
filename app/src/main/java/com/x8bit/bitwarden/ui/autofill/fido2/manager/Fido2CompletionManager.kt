package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface Fido2CompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: Fido2RegisterCredentialResult)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: Fido2CredentialAssertionResult)

    /**
     * Complete the FIDO 2 "Get credentials" process with the provided [result].
     */
    fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsResult)
}

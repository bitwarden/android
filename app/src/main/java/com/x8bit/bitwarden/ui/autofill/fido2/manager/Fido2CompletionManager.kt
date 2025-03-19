package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2AssertionCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2GetCredentialsCompletion
import com.x8bit.bitwarden.ui.autofill.fido2.manager.model.Fido2RegistrationCompletion

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface Fido2CompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: Fido2RegistrationCompletion)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: Fido2AssertionCompletion)

    /**
     * Complete the FIDO 2 "Get credentials" process with the provided [result].
     */
    fun completeFido2GetCredentialRequest(result: Fido2GetCredentialsCompletion)
}

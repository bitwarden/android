package com.x8bit.bitwarden.ui.autofill.credential.manager

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface CredentialCompletionManager {

    /**
     * Completes the FIDO 2 registration process with the provided [result].
     */
    fun completeFido2Registration(result: Fido2RegisterCredentialResult)

    /**
     * Complete the FIDO 2 credential assertion process with the provided [result].
     */
    fun completeFido2Assertion(result: Fido2CredentialAssertionResult)

    /**
     * Completes the Password registration process with the provided [result].
     */
    fun completePasswordRegistration(result: PasswordRegisterCredentialResult)

    /**
     * Completes the Password registration process with the provided [result].
     */
    fun completePasswordAssertion(result: PasswordCredentialAssertionResult)

    /**
     * Complete the FIDO 2 "Get credentials" process with the provided [result].
     */
    fun completeGetCredentialRequest(
        fido2Result: Fido2GetCredentialsResult?,
        passwordResult: PasswordGetCredentialsResult?,
    )
}

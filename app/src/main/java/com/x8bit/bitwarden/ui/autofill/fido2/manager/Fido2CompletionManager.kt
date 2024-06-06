package com.x8bit.bitwarden.ui.autofill.fido2.manager

import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialResult

/**
 * A manager for completing the FIDO 2 creation process.
 */
interface Fido2CompletionManager {

    /**
     * Completes the FIDO 2 creation process with the provided [result].
     */
    fun completeFido2Create(result: Fido2CreateCredentialResult)
}

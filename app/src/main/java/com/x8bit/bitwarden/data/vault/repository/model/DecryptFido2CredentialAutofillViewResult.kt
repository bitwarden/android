package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.fido.Fido2CredentialAutofillView

/**
 * Models result of decrypting the fido2 credential autofill views.
 */
sealed class DecryptFido2CredentialAutofillViewResult {
    /**
     * Credentials decrypted successfully.
     */
    data class Success(
        val fido2CredentialAutofillViews: List<Fido2CredentialAutofillView>,
    ) : DecryptFido2CredentialAutofillViewResult()

    /**
     * Generic error while decrypting credentials.
     */
    data object Error : DecryptFido2CredentialAutofillViewResult()
}

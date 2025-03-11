package com.x8bit.bitwarden.ui.autofill.fido2.manager.model

import androidx.credentials.provider.PublicKeyCredentialEntry
import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * Represents the completion state of a FIDO2 get credentials operation.
 */
sealed class Fido2GetCredentialsCompletion {

    /**
     * Indicates a successful completion, providing a list of [PublicKeyCredentialEntry].
     */
    data class Success(
        val entries: List<PublicKeyCredentialEntry>,
    ) : Fido2GetCredentialsCompletion()

    /**
     * Indicates an error during the operation, providing an error [message].
     *
     * @param message The error message.
     */
    data class Error(val message: Text) : Fido2GetCredentialsCompletion()

    /**
     * Indicates the cancellation of the operation by the user.
     */
    object Cancelled : Fido2GetCredentialsCompletion()
}

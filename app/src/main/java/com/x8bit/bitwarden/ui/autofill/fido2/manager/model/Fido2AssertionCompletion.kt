package com.x8bit.bitwarden.ui.autofill.fido2.manager.model

import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * Represents the completion of a FIDO2 assertion operation.
 */
sealed class Fido2AssertionCompletion {
    /**
     * Represents the successful completion of a FIDO2 assertion operation.
     *
     * @param responseJson The JSON response string from the FIDO2 assertion operation.
     */
    data class Success(val responseJson: String) : Fido2AssertionCompletion()

    /**
     * Represents an error during the FIDO2 assertion operation.
     *
     * @param message An optional human-readable message describing the error.
     */
    data class Error(val message: Text?) : Fido2AssertionCompletion()

    /**
     * Represents the cancellation of the FIDO2 assertion operation by the user.
     */
    object Cancelled : Fido2AssertionCompletion()
}

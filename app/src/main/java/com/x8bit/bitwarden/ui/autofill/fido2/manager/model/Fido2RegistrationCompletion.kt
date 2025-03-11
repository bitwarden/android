package com.x8bit.bitwarden.ui.autofill.fido2.manager.model

import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * Represents the result of a FIDO 2 registration.
 */
sealed class Fido2RegistrationCompletion {

    /**
     * Indicates registration was successful and a [responseJson] is available.
     */
    data class Success(val responseJson: String) : Fido2RegistrationCompletion()

    /**
     * Indicates registration encountered an error with an optional [message] that can be displayed
     * to the user.
     */
    data class Error(val message: Text?) : Fido2RegistrationCompletion()

    /**
     * Indicates registration was cancelled by the user.
     */
    object Cancelled : Fido2RegistrationCompletion()
}

package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of determining if a PIN is valid.
 */
sealed class ValidatePinResult {
    /**
     * The validity of the PIN was checked successfully and [isValid].
     */
    data class Success(
        val isValid: Boolean,
    ) : ValidatePinResult()

    /**
     * There was an error determining if the validity of the PIN.
     */
    data object Error : ValidatePinResult()
}

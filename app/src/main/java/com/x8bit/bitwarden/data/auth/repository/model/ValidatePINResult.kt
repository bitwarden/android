package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of determining if a PIN is valid.
 */
sealed class ValidatePINResult {
    /**
     * The validity of the PIN was checked successfully and [isValid].
     */
    data class Success(
        val isValid: Boolean,
    ) : ValidatePINResult()

    /**
     * There was an error determining if the validity of the PIN.
     */
    data object Error : ValidatePINResult()
}

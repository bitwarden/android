package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of determining if a password is valid.
 */
sealed class ValidatePasswordResult {

    /**
     * The validity of the password was checked successfully and [isValid].
     */
    data class Success(
        val isValid: Boolean,
    ) : ValidatePasswordResult()

    /**
     * There was an error determining if the validity of the password.
     */
    data object Error : ValidatePasswordResult()
}

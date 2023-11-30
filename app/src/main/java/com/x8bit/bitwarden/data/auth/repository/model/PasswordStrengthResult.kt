package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength

/**
 * Models result of determining the strength of a password.
 */
sealed class PasswordStrengthResult {
    /**
     * The contains the password strength.
     */
    data class Success(
        val passwordStrength: PasswordStrength,
    ) : PasswordStrengthResult()

    /**
     * There was an error determining the password strength.
     */
    data object Error : PasswordStrengthResult()
}

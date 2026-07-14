package com.x8bit.bitwarden.data.platform.manager.policy

import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation

/**
 * A manager for password policy requirements.
 */
interface PasswordPolicyManager {
    /**
     * Return the cached password policies for the current user.
     */
    val passwordPolicies: List<PolicyInformation.MasterPassword>

    /**
     * The reason for resetting the password.
     */
    val passwordResetReason: ForcePasswordResetReason?

    /**
     * Get the password strength for the given [email] and [password] combo. If no value is
     * passed for the [email] will use the active email of the current active account.
     */
    suspend fun getPasswordStrength(
        email: String? = null,
        password: String,
    ): PasswordStrengthResult

    /**
     * Remove the password to be validated against the Master Password Policy.
     */
    fun removePasswordToCheck(userId: String)

    /**
     * Store the password to be validated against the Master Password Policy.
     */
    fun storePasswordToCheck(userId: String, password: String)

    /**
     * Validates the given [password] against the master password policies for the current user.
     */
    suspend fun validatePasswordAgainstPolicies(password: String): Boolean
}

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
    fun getPasswordStrength(email: String? = null, password: String): PasswordStrengthResult

    /**
     * Validates the given [password] against the master password policies for the current user.
     */
    fun validatePasswordAgainstPolicies(password: String, isCreation: Boolean = true): Boolean

    /**
     * Return true if the user's master password passes the requirements of the
     * [PolicyInformation.MasterPassword].
     */
    fun passwordPassesPolicy(
        password: String,
        policyInfo: PolicyInformation.MasterPassword,
    ): Boolean
}

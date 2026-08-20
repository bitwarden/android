package com.x8bit.bitwarden.data.platform.manager.policy

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toInt
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies

/**
 * The default [PasswordPolicyManager] implementation. This class is responsible for validating
 * that password adhere to password policies.
 */
internal class PasswordPolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val authSdkSource: AuthSdkSource,
    private val policyManager: PolicyManager,
) : PasswordPolicyManager {
    private val activeAccountProfile: AccountJson.Profile?
        get() = authDiskSource.userState?.activeAccount?.profile

    override val passwordPolicies: List<PolicyInformation.MasterPassword>
        get() = policyManager.getActivePolicies()

    override val passwordResetReason: ForcePasswordResetReason?
        get() = activeAccountProfile?.forcePasswordResetReason

    override fun getPasswordStrength(
        email: String?,
        password: String,
    ): PasswordStrengthResult = authSdkSource
        .passwordStrength(
            email = email ?: activeAccountProfile?.email.orEmpty(),
            password = password,
        )
        .fold(
            onSuccess = { PasswordStrengthResult.Success(passwordStrength = it) },
            onFailure = { PasswordStrengthResult.Error(error = it) },
        )

    override fun validatePasswordAgainstPolicies(
        password: String,
        isCreation: Boolean,
    ): Boolean = passwordPolicies
        .filter {
            // We always enforce password policies when creating a new passowrd or if the policy
            // is being enforced on login.
            isCreation || it.enforceOnLogin == true
        }
        .all { validatePasswordAgainstPolicy(password = password, policy = it) }

    override fun passwordPassesPolicy(
        password: String,
        policyInfo: PolicyInformation.MasterPassword,
    ): Boolean = if (policyInfo.enforceOnLogin == true) {
        validatePasswordAgainstPolicy(password = password, policy = policyInfo)
    } else {
        // If the master password policy is not enabled and enforced on login, the check
        // should complete.
        true
    }

    @Suppress("CyclomaticComplexMethod")
    private fun validatePasswordAgainstPolicy(
        password: String,
        policy: PolicyInformation.MasterPassword,
    ): Boolean {
        // Check the password against all the enforced rules in the policy.
        policy.minLength?.let { minLength ->
            if (minLength > 0 && password.length < minLength) return false
        }
        policy.minComplexity?.let { minComplexity ->
            // If there was a problem checking the complexity of the password, ignore
            // the complexity checks and continue checking the other aspects of the policy.
            val profile = activeAccountProfile ?: return@let
            val passwordStrengthResult = getPasswordStrength(profile.email, password)
            val passwordStrength = (passwordStrengthResult as? PasswordStrengthResult.Success)
                ?.passwordStrength
                ?.toInt()
                ?: return@let
            if (minComplexity > 0 && passwordStrength < minComplexity) return false
        }
        policy.requireUpper?.let { requiresUpper ->
            if (requiresUpper && !password.any { it.isUpperCase() }) return false
        }
        policy.requireLower?.let { requiresLower ->
            if (requiresLower && !password.any { it.isLowerCase() }) return false
        }
        policy.requireNumbers?.let { requiresNumbers ->
            if (requiresNumbers && !password.any { it.isDigit() }) return false
        }
        policy.requireSpecial?.let { requiresSpecial ->
            if (requiresSpecial && !password.contains("^.*[!@#$%\\^&*].*$".toRegex())) return false
        }
        return true
    }
}

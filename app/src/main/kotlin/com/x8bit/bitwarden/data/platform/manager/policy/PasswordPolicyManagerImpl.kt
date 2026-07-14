package com.x8bit.bitwarden.data.platform.manager.policy

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.encryption.EncryptionManager
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toInt
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private const val ALIAS: String = "PasswordPolicyManager"

/**
 * The default [PasswordPolicyManager] implementation. This class is responsible for validating
 * that password adhere to password policies.
 */
internal class PasswordPolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val authSdkSource: AuthSdkSource,
    private val policyManager: PolicyManager,
    private val encryptionManager: EncryptionManager,
    dispatcherManager: DispatcherManager,
) : PasswordPolicyManager {

    private val unconfinedScope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    private val userState: UserStateJson? get() = authDiskSource.userState
    private val activeUserId: String? get() = userState?.activeUserId

    /**
     * The encrypted password that needs to be checked against any organization policies before
     * the user can complete the login flow. This value is stored using the user ID.
     */
    private val mutablePasswordsToCheckFlow = MutableStateFlow(value = mapOf<String, ByteArray>())

    init {
        // When the policies for the user have been set, complete the login process.
        combine(
            mutablePasswordsToCheckFlow,
            policyManager.getActivePoliciesFlow(type = PolicyType.MASTER_PASSWORD),
        ) { map, policies ->
            val userId = activeUserId ?: return@combine null
            val encryptedPasswordToCheck = map[userId] ?: return@combine null
            if (passwordResetReason != null) {
                // We are already forcing the user to reset the password, so there is no need to do
                // another check, and we can remove the password from memory.
                removePasswordToCheck(userId = userId)
                return@combine null
            }
            Triple(userId, encryptedPasswordToCheck, policies)
        }
            .filterNotNull()
            .onEach { (userId, encryptedPasswordToCheck, policies) ->
                encryptionManager
                    .decrypt(alias = ALIAS, bytes = encryptedPasswordToCheck)
                    .getOrNull()
                    ?.decodeToString()
                    ?.let { passwordToCheck ->
                        // Otherwise check the user's password against the policies and set or
                        // clear the force reset reason accordingly.
                        storeUserResetPasswordReason(
                            userId = userId,
                            reason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN.takeIf {
                                !passwordPassesPolicies(
                                    password = passwordToCheck,
                                    policies = policies,
                                )
                            },
                        )
                    }
                // We did the check, let's throw away the stored passowrd to keep it out of memory.
                removePasswordToCheck(userId = userId)
            }
            .launchIn(unconfinedScope)
        authDiskSource
            .userSwitchingChangesFlow
            .mapNotNull { it.previousActiveUserId }
            .onEach { userId -> removePasswordToCheck(userId = userId) }
            .launchIn(unconfinedScope)
    }

    override val passwordPolicies: List<PolicyInformation.MasterPassword>
        get() = policyManager.getActivePolicies()

    override val passwordResetReason: ForcePasswordResetReason?
        get() = userState?.activeAccount?.profile?.forcePasswordResetReason

    override suspend fun getPasswordStrength(
        email: String?,
        password: String,
    ): PasswordStrengthResult =
        authSdkSource
            .passwordStrength(
                email = email ?: userState?.activeAccount?.profile?.email.orEmpty(),
                password = password,
            )
            .fold(
                onSuccess = { PasswordStrengthResult.Success(passwordStrength = it) },
                onFailure = { PasswordStrengthResult.Error(error = it) },
            )

    override fun removePasswordToCheck(userId: String) {
        mutablePasswordsToCheckFlow.update { it.toMutableMap().apply { remove(key = userId) } }
    }

    override fun storePasswordToCheck(userId: String, password: String) {
        encryptionManager
            .encrypt(alias = ALIAS, bytes = password.encodeToByteArray())
            .getOrNull()
            ?.let { encryptedPassword ->
                mutablePasswordsToCheckFlow.update {
                    it.toMutableMap().apply { put(key = userId, value = encryptedPassword) }
                }
            }
    }

    override suspend fun validatePasswordAgainstPolicies(
        password: String,
    ): Boolean = passwordPolicies.all {
        validatePasswordAgainstPolicy(password = password, policy = it)
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun validatePasswordAgainstPolicy(
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
            val profile = userState?.activeAccount?.profile ?: return@let
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

    /**
     * Update the saved state with the force password reset reason.
     */
    private fun storeUserResetPasswordReason(userId: String, reason: ForcePasswordResetReason?) {
        val accounts = userState?.accounts?.toMutableMap() ?: return
        val account = accounts[userId] ?: return
        val updatedProfile = account.profile.copy(forcePasswordResetReason = reason)
        accounts[userId] = account.copy(profile = updatedProfile)
        authDiskSource.userState = userState?.copy(accounts = accounts)
    }

    /**
     * Return true if there are any [PolicyInformation.MasterPassword] policies that the user's
     * master password has failed to pass.
     */
    private suspend fun passwordPassesPolicies(
        password: String,
        policies: List<PolicyView>,
    ): Boolean {
        // If there are no master password policies that are enabled and should be
        // enforced on login, the check should complete.
        val passwordPolicies = policies
            .mapNotNull { it.policyInformation as? PolicyInformation.MasterPassword }
            .filter { it.enforceOnLogin == true }

        // Check the password against all the policies.
        return passwordPolicies.all { policy -> validatePasswordAgainstPolicy(password, policy) }
    }
}

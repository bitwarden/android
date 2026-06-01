package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Create a mock [PolicyInformation.MasterPassword] with a given parameters.
 */
@Suppress("LongParameterList")
fun createMockMasterPasswordPolicy(
    minLength: Int? = null,
    minComplexity: Int? = null,
    requireUpper: Boolean? = null,
    requireLower: Boolean? = null,
    requireNumbers: Boolean? = null,
    requireSpecial: Boolean? = null,
    enforceOnLogin: Boolean? = null,
): PolicyInformation.MasterPassword =
    PolicyInformation.MasterPassword(
        minLength = minLength,
        minComplexity = minComplexity,
        requireUpper = requireUpper,
        requireLower = requireLower,
        requireNumbers = requireNumbers,
        requireSpecial = requireSpecial,
        enforceOnLogin = enforceOnLogin,
    )

/**
 * Create a mock [PolicyInformation.VaultTimeout] with a given parameters.
 */
fun createMockVaultTimeoutPolicy(
    minutes: Int? = null,
    action: PolicyInformation.VaultTimeout.Action? = null,
    type: PolicyInformation.VaultTimeout.Type? = null,
): PolicyInformation.VaultTimeout =
    PolicyInformation.VaultTimeout(
        minutes = minutes,
        action = action,
        type = type,
    )

/**
 * Create a mock `String` representing a [PolicyInformation.VaultTimeout].
 */
fun createMockVaultTimeoutPolicyJsonString(
    vaultTimeout: PolicyInformation.VaultTimeout,
): String =
    """
      {
        "minutes":${vaultTimeout.minutes},
        "action":${vaultTimeout.toActionString?.let { "\"$it\"" }},
        "type":${vaultTimeout.toTypeString?.let { "\"$it\"" }}
      }
    """

private val PolicyInformation.VaultTimeout.toActionString: String?
    get() = when (this.action) {
        PolicyInformation.VaultTimeout.Action.LOCK -> "lock"
        PolicyInformation.VaultTimeout.Action.LOGOUT -> "logOut"
        null -> null
    }

private val PolicyInformation.VaultTimeout.toTypeString: String?
    get() = when (this.type) {
        PolicyInformation.VaultTimeout.Type.NEVER -> "never"
        PolicyInformation.VaultTimeout.Type.ON_APP_RESTART -> "onAppRestart"
        PolicyInformation.VaultTimeout.Type.ON_SYSTEM_LOCK -> "onSystemLock"
        PolicyInformation.VaultTimeout.Type.IMMEDIATELY -> "immediately"
        PolicyInformation.VaultTimeout.Type.CUSTOM -> "custom"
        null -> null
    }

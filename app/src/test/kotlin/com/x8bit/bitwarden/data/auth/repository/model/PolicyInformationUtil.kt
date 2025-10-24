package com.x8bit.bitwarden.data.auth.repository.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

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
 * Create a mock [JsonObject] representing a [PolicyInformation.VaultTimeout].
 */
fun createMockVaultTimeoutPolicyJsonObject(
    vaultTimeout: PolicyInformation.VaultTimeout,
): JsonObject =
    buildJsonObject {
        put(key = "minutes", element = JsonPrimitive(value = vaultTimeout.minutes))
        put(
            key = "action",
            element = JsonPrimitive(
                value = when (vaultTimeout.action) {
                    PolicyInformation.VaultTimeout.Action.LOCK -> "lock"
                    PolicyInformation.VaultTimeout.Action.LOGOUT -> "logOut"
                    null -> null
                },
            ),
        )
        put(
            key = "type",
            element = JsonPrimitive(
                value = when (vaultTimeout.type) {
                    PolicyInformation.VaultTimeout.Type.NEVER -> "never"
                    PolicyInformation.VaultTimeout.Type.ON_APP_RESTART -> "onAppRestart"
                    PolicyInformation.VaultTimeout.Type.ON_SYSTEM_LOCK -> "onSystemLock"
                    PolicyInformation.VaultTimeout.Type.IMMEDIATELY -> "immediately"
                    PolicyInformation.VaultTimeout.Type.CUSTOM -> "custom"
                    null -> null
                },
            ),
        )
    }

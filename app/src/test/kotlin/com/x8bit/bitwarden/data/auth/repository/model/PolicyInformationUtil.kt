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

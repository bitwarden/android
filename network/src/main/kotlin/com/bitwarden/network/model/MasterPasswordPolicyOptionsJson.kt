package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Policies that may be applied to a master password.
 *
 * @property minimumComplexity The minimum required password complexity (if applicable).
 * @property minimumLength The minimum required password length (if applicable).
 * @property shouldRequireUppercase Whether uppercase characters should be required.
 * @property shouldRequireLowercase Whether lowercase characters should be required.
 * @property shouldRequireNumbers Whether numbers should be required.
 * @property shouldRequireSpecialCharacters Whether special characters should be required.
 * @property shouldEnforceOnLogin Whether the restrictions should be enforced on login.
 */
@Serializable
data class MasterPasswordPolicyOptionsJson(
    @SerialName("MinComplexity")
    val minimumComplexity: Int?,

    @SerialName("MinLength")
    val minimumLength: Int?,

    @SerialName("RequireUpper")
    val shouldRequireUppercase: Boolean?,

    @SerialName("RequireLower")
    val shouldRequireLowercase: Boolean?,

    @SerialName("RequireNumbers")
    val shouldRequireNumbers: Boolean?,

    @SerialName("RequireSpecial")
    val shouldRequireSpecialCharacters: Boolean?,

    @SerialName("EnforceOnLogin")
    val shouldEnforceOnLogin: Boolean?,
)

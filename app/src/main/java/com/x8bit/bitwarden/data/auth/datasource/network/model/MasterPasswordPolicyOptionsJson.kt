package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Policies that may be applied to a master password.
 *
 * @property minimumComplexity The minimum required password complexity (if applicable).
 * @property minimumLength The minimum required password length (if applicable).
 * @property shouldRequireUppercase Whether or not uppercase characters should be required.
 * @property shouldRequireLowercase Whether or not lowercase characters should be required.
 * @property shouldRequireNumbers Whether or not numbers should be required.
 * @property shouldRequireSpecialCharacters Whether or not special characters should be required.
 * @property shouldEnforceOnLogin Whether or not the restrictions should be enforced on login.
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
